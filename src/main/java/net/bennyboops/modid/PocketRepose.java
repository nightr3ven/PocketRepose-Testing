package net.bennyboops.modid;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.bennyboops.modid.block.ModBlocks;
import net.bennyboops.modid.block.SuitcaseBlock;
import net.bennyboops.modid.block.entity.ModBlockEntities;
import net.bennyboops.modid.block.entity.SuitcaseBlockEntity;
import net.bennyboops.modid.criterion.EnterPocketDimensionCriterion;
import net.bennyboops.modid.data.PlayerEntryData;
import net.bennyboops.modid.data.MobEntryData;
import net.bennyboops.modid.data.SuitcaseRegistrySavedData;
import net.bennyboops.modid.item.KeystoneItem;
import net.bennyboops.modid.item.ModItemGroups;
import net.bennyboops.modid.item.ModItems;
import net.bennyboops.modid.world.PortalChunkGenerator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeLevelConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class PocketRepose implements ModInitializer {

	public static final Identifier POCKET_DIMENSION_TYPE_ID =
			Identifier.fromNamespaceAndPath("pocket-repose", "pocket_dimension_type");
	public static final ResourceKey<DimensionType> POCKET_DIMENSION_TYPE_KEY =
			ResourceKey.create(Registries.DIMENSION_TYPE, POCKET_DIMENSION_TYPE_ID);
	public static final String MOD_ID = "pocket-repose";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final EnterPocketDimensionCriterion ENTER_POCKET_DIMENSION = new EnterPocketDimensionCriterion();


	@Override
	public void onInitialize() {

		LOGGER.info("Initializing " + MOD_ID);

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			SuitcaseRegistrySavedData.onServerStart(server);
		});

		Registry.register(
				BuiltInRegistries.TRIGGER_TYPES,
				Identifier.fromNamespaceAndPath(MOD_ID, "enter_pocket_dimension"),
				ENTER_POCKET_DIMENSION
		);

		registerPocketCommands();
		registerSuitcaseMobTeleport();
		registerMobEntrySetter();
		registerPlayerEntrySetter();
		registerKeyRescueMob();

		ModItems.registerModItems();
		ModBlocks.registerModBlocks();
		ModItemGroups.registerItemGroups();
		ModBlockEntities.registerBlockEntities();

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			Path registryFile = server.getWorldPath(LevelResource.ROOT)
					.resolve("data")
					.resolve("pocket-repose")
					.resolve("dimension_registry")
					.resolve("registry.txt");

			if (!Files.exists(registryFile)) {
				return;
			}
			Registry<Biome> biomeRegistry = server.registryAccess().lookupOrThrow(Registries.BIOME);
			ResourceKey<Biome> voidBiomeKey =
					ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("pocket-repose", "pocket_islands"));

			long seed = server.overworld().getSeed();
			try {
				for (String dimName : Files.readAllLines(registryFile)) {
					Identifier worldId = Identifier.fromNamespaceAndPath("pocket-repose", dimName);

					ChunkGenerator voidGen = new PortalChunkGenerator(biomeRegistry);

					RuntimeLevelConfig cfg = new RuntimeLevelConfig()
							.setDimensionType(POCKET_DIMENSION_TYPE_KEY)
							.setGenerator(voidGen)
							.setSeed(seed);
					Fantasy.get(server).getOrOpenPersistentLevel(worldId, cfg);
				}
			} catch (IOException e) {
				PocketRepose.LOGGER.error("Failed to reload pocket dimensions", e);
			}
		});

		// Initialize player entry location
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
					LiteralArgumentBuilder.<CommandSourceStack>literal("pocketRepose")
							.then(Commands.literal("setPlayerEntry")
									.executes(ctx -> {
										CommandSourceStack src = ctx.getSource();
										ServerLevel world = src.getLevel();
										Identifier id = world.dimension().identifier();
										if (!"pocket-repose".equals(id.getNamespace())
												|| !id.getPath().startsWith("pocket_dimension_")) {
											src.sendFailure(Component.literal("§cNot in a pocket dimension"));
											return 0;
										}
										Vec3 pos = src.getPosition();
										float yaw = src.getEntity().getYRot();
										float pitch = src.getEntity().getXRot();

										net.bennyboops.modid.data.PlayerEntryData.get(world)
												.setEntry(pos, yaw, pitch);

										src.sendSuccess(() -> Component.literal(
												String.format("§aPlayer entry set to %.2f, %.2f, %.2f",
														pos.x, pos.y, pos.z)
										), false);
										return 1;
									})));
		});
	}


	public static final ResourceKey<Biome> VOID_BIOME_KEY =
			ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("pocket-repose", "pocket_islands"));


	private void registerPocketCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(
					LiteralArgumentBuilder.<CommandSourceStack>literal("pocketRepose")

							//mob entry setter: /pocketRepose setmobentry
							.then(Commands.literal("setMobEntry")
									.executes(ctx -> {
										CommandSourceStack src = ctx.getSource();
										ServerLevel world = src.getLevel();
										Identifier id = world.dimension().identifier();
										if (!id.getNamespace().equals("pocket-repose")
												|| !id.getPath().startsWith("pocket_dimension_")) {
											src.sendFailure(Component.literal("§cNot in a pocket dimension"));
											return 0;
										}
										Vec3 pos = src.getPosition();
										float yaw = src.getEntity().getYRot();
										float pitch = src.getEntity().getXRot();

										MobEntryData.get(world).setEntry(pos, yaw, pitch);
										src.sendSuccess(() -> Component.literal(
												String.format("§aMob entry set to %.2f, %.2f, %.2f", pos.x, pos.y, pos.z)
										), false);
										return 1;
									})
							)
							//player entry setter: /pocketRepose setplayerentry
							.then(Commands.literal("setPlayerEntry")
									.executes(ctx -> {
										CommandSourceStack src = ctx.getSource();
										ServerLevel world = src.getLevel();
										Identifier id = world.dimension().identifier();
										if (!id.getNamespace().equals("pocket-repose")
												|| !id.getPath().startsWith("pocket_dimension_")) {
											src.sendFailure(Component.literal("§cNot in a pocket dimension"));
											return 0;
										}
										Vec3 pos = src.getPosition();
										float yaw = src.getEntity().getYRot();
										float pitch = src.getEntity().getXRot();

										PlayerEntryData.get(world).setEntry(pos, yaw, pitch);
										src.sendSuccess(() -> Component.literal(
												String.format("§aPlayer entry set to %.2f, %.2f, %.2f", pos.x, pos.y, pos.z)
										), false);
										return 1;
									})
							)

							//reset command
							.then(Commands.literal("resetPlayerEntry")
									.then(Commands.argument("dimension", StringArgumentType.word())
											.executes(ctx -> resetPocketDimension(ctx, StringArgumentType.getString(ctx, "dimension")))
									)
							)

							//list command. only OPs with permission level 2+ can run
							.then(Commands.literal("listDimensions")
									.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
									.executes(ctx -> {
										CommandSourceStack src = ctx.getSource();
										src.sendSuccess(() -> Component.literal("§aPocket Dimensions Loaded:"), false);
										boolean foundAny = false;
										for (ServerLevel world : src.getServer().getAllLevels()) {
											Identifier id = world.dimension().identifier();
											String namespace = id.getNamespace();
											String path = id.getPath();
											String prefix = "pocket_dimension_";

											if ("pocket-repose".equals(namespace) && path.startsWith(prefix)) {
												String suffix = path.substring(prefix.length());
												src.sendSuccess(() -> Component.literal(" " + suffix), false);
												foundAny = true;
											}
										}
										if (!foundAny) {
											src.sendSuccess(() -> Component.literal("§cNo pocket dimensions found."), false);
										}
										return 1;
									})
							)

							// New command: /pocketRepose canCaptureHostile true/false
							.then(Commands.literal("canCaptureHostile")
									.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
									.then(Commands.argument("value", BoolArgumentType.bool())
											.executes(ctx -> {
												boolean value = BoolArgumentType.getBool(ctx, "value");
												setCanCaptureHostile(value);
												CommandSourceStack src = ctx.getSource();
												src.sendSuccess(() -> Component.literal(
														value ? "§aHostile mob capture enabled" : "§cHostile mob capture disabled"
												), false);
												return 1;
											})
									)
									.executes(ctx -> {
										// Show current status when no argument is provided
										CommandSourceStack src = ctx.getSource();
										boolean current = getCanCaptureHostile();
										src.sendSuccess(() -> Component.literal(
												"§7Hostile mob capture is currently: " + (current ? "§aEnabled" : "§cDisabled")
										), false);
										return 1;
									})
							)

							// Blacklist commands
							.then(Commands.literal("mobBlacklist")
									// Add entity to blacklist: /pocketRepose mobBlacklist add <entity>
									.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
									.then(Commands.literal("add")
											.then(Commands.argument("entity", StringArgumentType.string())
													.executes(ctx -> {
														CommandSourceStack src = ctx.getSource();
														String entityString = StringArgumentType.getString(ctx, "entity");

														try {
													Identifier entityId = Identifier.parse(entityString);
															EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(entityId);

															if (entityType == null) {
																// Default fallback means entity doesn't exist
																src.sendFailure(Component.literal("§cUnknown entity type: " + entityString));
																return 0;
															}

															if (entityType == EntityTypes.PLAYER) {
																src.sendFailure(Component.literal("§cCannot blacklist players"));
																return 0;
															}

															addToBlacklist(entityType);
															src.sendSuccess(() -> Component.literal(
																	"§aAdded " + entityId + " to blacklist"
															), false);
															return 1;
														} catch (Exception e) {
															src.sendFailure(Component.literal("§cInvalid entity identifier: " + entityString));
															return 0;
														}
													})
											)
									)
									// Remove entity from blacklist: /pocketRepose mobBlacklist remove <entity>
									.then(Commands.literal("remove")
											.then(Commands.argument("entity", StringArgumentType.string())
													.executes(ctx -> {
														CommandSourceStack src = ctx.getSource();
														String entityString = StringArgumentType.getString(ctx, "entity");

														try {
													Identifier entityId = Identifier.parse(entityString);
															EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(entityId);

															if (entityType == null) {
																// Default fallback means entity doesn't exist
																src.sendFailure(Component.literal("§cUnknown entity type: " + entityString));
																return 0;
															}

															boolean removed = removeFromBlacklist(entityType);
															if (removed) {
																src.sendSuccess(() -> Component.literal(
																		"§aRemoved " + entityId + " from blacklist"
																), false);
															} else {
																src.sendSuccess(() -> Component.literal(
																		"§7" + entityId + " was not in blacklist"
																), false);
															}
															return 1;
														} catch (Exception e) {
															src.sendFailure(Component.literal("§cInvalid entity identifier: " + entityString));
															return 0;
														}
													})
											)
									)
									// List blacklisted entities: /pocketRepose mobBlacklist list
									.then(Commands.literal("list")
											.executes(ctx -> {
												CommandSourceStack src = ctx.getSource();
												Set<EntityType<?>> blacklist = getEntityBlacklist();

												if (blacklist.isEmpty()) {
													src.sendSuccess(() -> Component.literal("§7No entities are blacklisted"), false);
												} else {
													src.sendSuccess(() -> Component.literal("§aBlacklisted entities:"), false);
													blacklist.forEach(entityType -> {
												Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
														src.sendSuccess(() -> Component.literal(" " + id), false);
													});
												}
												return 1;
											})
									)
									// Clear blacklist: /pocketRepose mobBlacklist clear
									.then(Commands.literal("clear")
											.executes(ctx -> {
												CommandSourceStack src = ctx.getSource();
												int count = getEntityBlacklist().size();
												clearBlacklist();
												src.sendSuccess(() -> Component.literal(
														"§aCleared blacklist (removed " + count + " entities)"
												), false);
												return 1;
											})
									)
							)
			);
		});
	}


	private int resetPocketDimension(CommandContext<CommandSourceStack> ctx, String dimSuffix) {
		CommandSourceStack src = ctx.getSource();

		Identifier dimId = Identifier.fromNamespaceAndPath("pocket-repose", "pocket_dimension_" + dimSuffix);
		ResourceKey<Level> worldKey = ResourceKey.create(Registries.DIMENSION, dimId);
		ServerLevel targetWorld = src.getServer().getLevel(worldKey);

		if (targetWorld == null) {
			src.sendFailure(Component.literal("§cPocket dimension '"
					+ dimSuffix + "' not found"));
			return 0;
		}

		BlockPos plankPos = new BlockPos(17, 96, 9);
		targetWorld.setBlockAndUpdate(plankPos, Blocks.OAK_PLANKS.defaultBlockState());

		for (int y = 97; y <= 99; y++) {
			BlockPos airPos = new BlockPos(17, y, 9);
			targetWorld.setBlockAndUpdate(airPos, Blocks.AIR.defaultBlockState());
		}

		BlockPos portalPos = new BlockPos(17, 100, 9);
		targetWorld.setBlockAndUpdate(portalPos, ModBlocks.PORTAL.defaultBlockState());

		PlayerEntryData playerData = PlayerEntryData.get(targetWorld);
		playerData.setEntry(new Vec3(17.5, 97.0, 9.5), 0f, 0f);

		src.sendSuccess(() -> Component.literal(
				"§aPocket dimension '" + dimSuffix + "' entry reset"
		), false);

		return 1;
	}


	private void registerPlayerEntrySetter() {
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack s = player.getItemInHand(hand);
			if (world.isClientSide() || hand != InteractionHand.MAIN_HAND || s.getItem() != Items.BONE)
				return InteractionResult.PASS;

			if (!(world instanceof ServerLevel sw)) return InteractionResult.PASS;
			Identifier id = sw.dimension().identifier();
			if (!"pocket-repose".equals(id.getNamespace())
					|| !id.getPath().startsWith("pocket_dimension_"))
				return InteractionResult.PASS;

			Vec3 pos = player.position();
			float yaw = player.getYRot();
			float pitch = player.getXRot();
			PlayerEntryData.get(sw).setEntry(pos, yaw, pitch);

			player.sendOverlayMessage(Component.literal(
					String.format("§aPlayer entry location set to %.1f, %.1f, %.1f",
							pos.x, pos.y, pos.z)
			));

			return InteractionResult.SUCCESS;
		});
	}

	private void registerMobEntrySetter() {
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (world.isClientSide() || hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			if (stack.getItem() != Items.LEAD) {
				return InteractionResult.PASS;
			}
			if (!(world instanceof ServerLevel sw)) {
				return InteractionResult.PASS;
			}
			Identifier id = sw.dimension().identifier();
			if (!"pocket-repose".equals(id.getNamespace())
					|| !id.getPath().startsWith("pocket_dimension_")) {
				return InteractionResult.PASS;
			}

			Vec3 pos = player.position();
			float yaw = player.getYRot();
			float pitch = player.getXRot();
			MobEntryData.get(sw).setEntry(pos, yaw, pitch);

			player.sendOverlayMessage(Component.literal(
					String.format("§aMob entry location set to %.1f, %.1f, %.1f",
							pos.x, pos.y, pos.z)
			));

			return InteractionResult.SUCCESS;
		});
	}

	private static boolean canCaptureHostile = false;
	private static Set<EntityType<?>> entityBlacklist = new HashSet<>();

	public static boolean getCanCaptureHostile() {
		return canCaptureHostile;
	}

	public static void setCanCaptureHostile(boolean value) {
		canCaptureHostile = value;
	}

	public static Set<EntityType<?>> getEntityBlacklist() {
		return new HashSet<>(entityBlacklist);
	}

	public static void addToBlacklist(EntityType<?> entityType) {
		entityBlacklist.add(entityType);
	}

	public static boolean removeFromBlacklist(EntityType<?> entityType) {
		return entityBlacklist.remove(entityType);
	}

	public static boolean isBlacklisted(EntityType<?> entityType) {
		return entityBlacklist.contains(entityType);
	}

	public static void clearBlacklist() {
		entityBlacklist.clear();
	}

	private boolean isHostileMob(LivingEntity mob) {
		return mob instanceof Monster ||
				mob instanceof Spider ||
				mob instanceof EnderMan ||
				mob instanceof Piglin ||
				mob instanceof ZombifiedPiglin ||
				(mob instanceof Wolf wolf && wolf.isAngry());
	}

	private void registerSuitcaseMobTeleport() {
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (world.isClientSide()) return InteractionResult.PASS;
			ItemStack stack = player.getItemInHand(hand);
			if (!(stack.getItem() instanceof BlockItem bi)) {
				return InteractionResult.PASS;
			}
			Block heldBlock = bi.getBlock();
			if (!(heldBlock instanceof SuitcaseBlock)) {
				return InteractionResult.PASS;
			}
			TypedEntityData<?> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
			CompoundTag beNbt = blockEntityData != null && blockEntityData.type() == ModBlockEntities.SUITCASE_BLOCK_ENTITY
					? blockEntityData.copyTagWithoutId() : null;
			if (beNbt == null || !beNbt.contains("BoundKeystone")) {
				player.sendOverlayMessage(Component.literal("§c☒"));
				world.playSound(
						null,
						player.getX(), player.getY(), player.getZ(),
						SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
						SoundSource.PLAYERS,
						0.3f, 1.5f
				);
				return InteractionResult.FAIL;
			}
			if (beNbt.getBooleanOr("Locked", false)) {
				player.sendOverlayMessage(Component.literal("§c☒"));
				world.playSound(
						null,
						player.getX(), player.getY(), player.getZ(),
						SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
						SoundSource.PLAYERS,
						0.3f, 1.5f
				);
				return InteractionResult.FAIL;
			}
			String keystone = beNbt.getStringOr("BoundKeystone", "");
			Identifier dimId = Identifier.fromNamespaceAndPath("pocket-repose", "pocket_dimension_" + keystone);
			ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimId);
			ServerLevel targetWorld = world.getServer().getLevel(dimKey);
			if (targetWorld == null) {
				player.sendOverlayMessage(Component.literal("§cPocket dimension not found"));
				return InteractionResult.FAIL;
			}
			if (!(entity instanceof LivingEntity mob)) {
				return InteractionResult.PASS;
			}
			// Check if mob is blacklisted
			if (isBlacklisted(mob.getType())) {
				player.sendOverlayMessage(Component.literal("§c☒"));
				world.playSound(
						null,
						player.getX(), player.getY(), player.getZ(),
						SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
						SoundSource.PLAYERS,
						0.3f, 1.5f
				);
				return InteractionResult.FAIL;
			}
			// Check if mob is hostile and if hostile capture is disabled
			if (!canCaptureHostile && isHostileMob(mob)) {
				player.sendOverlayMessage(Component.literal("§c☒"));
				world.playSound(
						null,
						player.getX(), player.getY(), player.getZ(),
						SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
						SoundSource.PLAYERS,
						0.3f, 1.5f
				);
				return InteractionResult.FAIL;
			}
			MobEntryData data = MobEntryData.get(targetWorld);
			Vec3 dest   = data.getEntryPos();
			float yaw    = data.getEntryYaw();
			float pitch  = data.getEntryPitch();
			TeleportTransition transition = new TeleportTransition(
					targetWorld, dest, Vec3.ZERO, yaw, pitch, TeleportTransition.DO_NOTHING
			);
			mob.teleport(transition);
			world.playSound(
					null,
					player.getX(), player.getY(), player.getZ(),
					SoundEvents.BUNDLE_DROP_CONTENTS,
					SoundSource.PLAYERS,
					2.0f, 1.0f
			);
			world.playSound(
					null,
					player.getX(), player.getY(), player.getZ(),
					SoundEvents.ITEM_PICKUP,
					SoundSource.PLAYERS,
					0.5f, 1.0f
			);
			return InteractionResult.SUCCESS;
		});
	}

	private void registerKeyRescueMob() {
		UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (world.isClientSide()) {
				return InteractionResult.PASS;
			}
			ItemStack held = player.getItemInHand(hand);
			if (!(held.getItem() instanceof KeystoneItem)) {
				return InteractionResult.PASS;
			}
			Identifier dimId = world.dimension().identifier();
			String namespace = dimId.getNamespace();
			String path      = dimId.getPath();
			String prefix    = "pocket_dimension_";
			if (!namespace.equals("pocket-repose") || !path.startsWith(prefix)) {
				return InteractionResult.PASS;
			}
			String keystoneName = path.substring(prefix.length());
			if (!(entity instanceof LivingEntity mob)) {
				return InteractionResult.PASS;
			}
			String playerUuid = player.getStringUUID();
			BlockPos suitcasePos = SuitcaseBlockEntity.findSuitcasePosition(keystoneName, playerUuid);
			if (suitcasePos == null) {
				player.sendOverlayMessage(Component.literal("§cNo suitcase found"));
				return InteractionResult.FAIL;
			}
			ServerLevel overworld = world.getServer().getLevel(Level.OVERWORLD);
			if (overworld == null) {
				player.sendOverlayMessage(Component.literal("§cOverworld is not loaded"));
				return InteractionResult.FAIL;
			}
			Vec3 exitPos = new Vec3(
					suitcasePos.getX() + 0.5,
					suitcasePos.getY() + 0.5,
					suitcasePos.getZ() + 0.5
			);
			float yaw   = mob.getYRot();
			float pitch = mob.getXRot();
			TeleportTransition transition = new TeleportTransition(
					overworld, exitPos, Vec3.ZERO, yaw, pitch, TeleportTransition.DO_NOTHING
			);
			mob.teleport(transition);
			overworld.playSound(
					null,
					exitPos.x, exitPos.y, exitPos.z,
					SoundEvents.BUNDLE_DROP_CONTENTS,
					SoundSource.PLAYERS,
					2.0f, 1.0f
			);
			world.playSound(
					null,
					player.getX(), player.getY(), player.getZ(),
					SoundEvents.BUNDLE_DROP_CONTENTS,
					SoundSource.PLAYERS,
					2.0f, 1.0f
			);

			return InteractionResult.SUCCESS;
		});
	}
}
