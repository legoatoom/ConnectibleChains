package com.github.legoatoom.connectiblechains.entity;

public class ChainKnotEntityOld {
//
//
//    /**
//     * The distance when it is visible.
//     */
//    @Deprecated
//    public static final double VISIBLE_RANGE = 2048.0D;
//    /**
//     * Ticks where the knot can live without any links.
//     * This is important for 2 reasons: When the world loads, a 'secondary' knot might load before it's 'primary'
//     * In which case the knot would remove itself as it has no links and when the 'primary' loads it fails to create
//     * a link to this as this is already removed. The second use is for /summon for basically the same reasons.
//     */
//    @Deprecated
//    private static final byte GRACE_PERIOD = 100;
//    private final static String SOURCE_ITEM_KEY = "SourceItem";
//    /**
//     * All links that involve this knot (secondary and primary)
//     */
//    @Deprecated
//    private final ObjectList<ChainLink> links = new ObjectArrayList<>();
//    /**
//     * Links where the 'secondary' might not exist yet. Will be cleared after the grace period.
//     */
//    private final ObjectList<NbtElement> incompleteLinks = new ObjectArrayList<>();
//    /**
//     * Increments each tick, when it reached 100 it resets and checks {@link #canStayAttached()}.
//     */
//    @Deprecated
//    private int obstructionCheckTimer = 0;
//    /**
//     * The chain type, used for rendering
//     */
//    @Deprecated
//    private Item chainItemSource = Items.CHAIN;
//    /**
//     * Remaining grace ticks, will be set to 0 when the last incomplete link is removed.
//     */
//    @Deprecated
//    private byte graceTicks = GRACE_PERIOD;
//    /**
//     * What block the knot is attached to.
//     */
//    @Environment(EnvType.CLIENT)
//    @Deprecated
//    private BlockState attachTarget;
//
//    /**
//     * The maximum distance between two knots.
//     */
//    public static double getMaxRange() {
//        return ConnectibleChains.runtimeConfig.getMaxChainRange();
//    }
//
//    /**
//     * Is this block acceptable to attach a knot?
//     *
//     * @param blockState The state of the block in question.
//     * @return true if is allowed.
//     */
//    public static boolean canAttachTo(BlockState blockState) {
//        if (blockState != null) {
//            return blockState.isIn(ModTagRegistry.CHAIN_CONNECTIBLE);
//        }
//        return false;
//    }
//
//    /**
//     * Searches for other {@link ChainKnotEntity ChainKnotEntities} that are in range of {@code target} and
//     * have a link to {@code player}.
//     *
//     * @param player the player wo tries to make a connection.
//     * @param target center of the range
//     * @return a list of all held chains that are in range of {@code target}
//     */
//    public static List<ChainLink> getHeldChainsInRange(PlayerEntity player, BlockPos target) {
//        Box searchBox = Box.of(Vec3d.of(target), getMaxRange() * 2, getMaxRange() * 2, getMaxRange() * 2);
//        List<ChainKnotEntity> otherKnots = player.getWorld().getNonSpectatingEntities(ChainKnotEntity.class, searchBox);
//
//        List<ChainLink> attachableLinks = new ArrayList<>();
//
//        for (ChainKnotEntity source : otherKnots) {
//            for (ChainLink link : source.getLinks()) {
//                if (link.getSecondary() != player) continue;
//                // We found a knot that is connected to the player.
//                attachableLinks.add(link);
//            }
//        }
//        return attachableLinks;
//    }
//
//    /**
//     * On the server it:
//     * <ol>
//     * <li>Checks if its in the void and deletes itself.</li>
//     * <li>Tries to convert incomplete links</li>
//     * <li>Updates the chains, see {@link #updateLinks()}</li>
//     * <li>Removes any dead links, and, when outside the grace period, itself if none are left.</li>
//     * </ol>
//     */
//    @Override
//    public void tick() {
//        if (getWorld().isClient()) {
//            // All other logic in handled on the server. The client only knows enough to render the entity.
//            links.removeIf(ChainLink::isDead);
//            attachTarget = getWorld().getBlockState(attachedBlockPos);
//            return;
//        }
//        attemptTickInVoid();
//
//        boolean anyConverted = convertIncompleteLinks();
//        updateLinks();
//        removeDeadLinks();
//
//        if (graceTicks < 0 || (anyConverted && incompleteLinks.isEmpty())) {
//            graceTicks = 0;
//        } else if (graceTicks > 0) {
//            graceTicks--;
//        }
//    }
//
//    /**
//     * Will try to convert any {@link #incompleteLinks} using {@link #deserializeChainTag(NbtElement)}.
//     *
//     * @return true if any were converted
//     */
//    private boolean convertIncompleteLinks() {
//        if (!incompleteLinks.isEmpty()) {
//            return incompleteLinks.removeIf(this::deserializeChainTag);
//        }
//        return false;
//    }
//
//    /**
//     * Will break all connections that are larger than the {@link #getMaxRange()},
//     * when this knot is dead, or can't stay attached.
//     */
//    private void updateLinks() {
//        double squaredMaxRange = getMaxRange() * getMaxRange();
//        for (ChainLink link : links) {
//            if (link.isDead()) continue;
//
//            if (!isAlive()) {
//                link.destroy(true);
//            } else if (link.getPrimary() == this && link.getSquaredDistance() > squaredMaxRange) {
//                // no need to check the distance on both ends
//                link.destroy(true);
//            }
//        }
//
//        if (obstructionCheckTimer++ == 100) {
//            obstructionCheckTimer = 0;
//            if (!canStayAttached()) {
//                destroyLinks(true);
//            }
//        }
//    }
//
//    /**
//     * Removes any dead links and plays a break sound if any were removed.
//     * Removes itself when no {@link #links} or {@link #incompleteLinks} are left, and it's outside the grace period.
//     */
//    private void removeDeadLinks() {
//        boolean playBreakSound = false;
//        for (ChainLink link : links) {
//            if (link.needsBeDestroyed()) link.destroy(true);
//            if (link.isDead() && !link.removeSilently) playBreakSound = true;
//        }
//        if (playBreakSound) onBreak(null);
//
//        links.removeIf(ChainLink::isDead);
//        if (links.isEmpty() && incompleteLinks.isEmpty() && graceTicks <= 0) {
//            remove(RemovalReason.DISCARDED);
//            // No break sound
//        }
//    }
//
//    /**
//     * This method tries to connect to the secondary that is in the {@link #incompleteLinks}.
//     * If they do not exist yet, we try again later. If they do, make a connection and remove it from the tag.
//     * <br>
//     * When the grace period is over, we remove the tag from the {@link #incompleteLinks} and drop an item
//     * meaning that we cannot find the connection anymore, and we assume that it will not be loaded in the future.
//     *
//     * @param element the tag that contains a single connection.
//     * @return true if the tag has been used
//     * @see #updateLinks()
//     */
//    private boolean deserializeChainTag(NbtElement element) {
//        if (element == null || getWorld().isClient()) {
//            return true;
//        }
//
//        assert element instanceof NbtCompound;
//        NbtCompound tag = (NbtCompound) element;
//
//        Item source = Registries.ITEM.get(Identifier.tryParse(tag.getString(SOURCE_ITEM_KEY)));
//
//        if (tag.contains("UUID")) {
//            UUID uuid = tag.getUuid("UUID");
//            Entity entity = ((ServerWorld) getWorld()).getEntity(uuid);
//            if (entity != null) {
//                ChainLink.create(this, entity, source);
//                return true;
//            }
//        } else if (tag.contains("RelX") || tag.contains("RelY") || tag.contains("RelZ")) {
//            BlockPos blockPos = new BlockPos(tag.getInt("RelX"), tag.getInt("RelY"), tag.getInt("RelZ"));
//            // Adjust position to be relative to our facing direction
//            blockPos = getBlockPosAsFacingRelative(blockPos, Direction.fromHorizontalDegrees(this.getYaw()));
//            ChainKnotEntity entity = ChainKnotEntity.getKnotAt(getWorld(), blockPos.add(attachedBlockPos));
//            if (entity != null) {
//                ChainLink.create(this, entity, source);
//                return true;
//            }
//        } else {
//            ConnectibleChains.LOGGER.warn("Chain knot NBT is missing UUID or relative position.");
//        }
//
//        // TODO: 18/11/2022 Issue #31 maybe here? It could be that we need to check if connection chunk is loaded or not.
//
//        // At the start the server and client need to tell each other the info.
//        // So we need to check if the object is old enough for these things to exist before we delete them.
//        if (graceTicks <= 0) {
//            dropItem(source);
//            onBreak(null);
//            return true;
//        }
//
//        return false;
//    }
//
//    /**
//     * Simple checker to see if the block is connected to a fence or a wall.
//     *
//     * @return true if it can stay attached.
//     */
//    @Override
//    public boolean canStayAttached() {
//        return canAttachTo(this.getWorld().getBlockState(this.attachedBlockPos));
//    }
//
//    @Override
//    public void onStartedTrackingBy(ServerPlayerEntity player) {
//        return; // TODO: See if this is still needed?
//
//        ServerPlayNetworking.send(player,
//                new MultiChainAttachPayload(
//                        this.getLinks()
//                                .stream()
//                                .filter(chainLink -> chainLink.getPrimary().getId() == this.getId())
//                                .map(chainLink -> new ChainAttachS2CPacket(chainLink, true))
//                                .toList()));
//    }
//
//    /**
//     * Calls {@link #damage} when attacked by a player. Plays a hit sound otherwise. <br/>
//     * It is used by {@link PlayerEntity#attack} where a true return value indicates
//     * that this entity handled the attack and no further actions should be made.
//     *
//     * @param attacker The source of the attack.
//     * @return true
//     */
//    @Override
//    public boolean handleAttack(Entity attacker) {
//        if (!super.handleAttack(attacker)) {
//            playSound(getSoundGroup().getHitSound(), 0.5F, 1.0F);
//        }
//        return true;
//    }
//
//    /**
//     * @return true when damage was effective
//     * @see ChainKnotEntity#onDamageFrom
//     */
//    @Override
//    public boolean damage(ServerWorld world, DamageSource source, float amount) {
//        ActionResult result = ChainLinkEntity.onDamageFrom(this, source, getSoundGroup().getHitSound());
//
//        if (result.isAccepted()) {
//            destroyLinks(result == ActionResult.SUCCESS);
//            return true;
//        }
//        return false;
//    }
//
//    /**
//     * Stores the {@link #chainItemSource chain type} and all primary links
//     * and old, incomplete links inside {@code root}
//     *
//     * @param root the tag to write info in.
//     */
//    @Override
//    public void writeCustomDataToNbt(NbtCompound root) {
//        root.putString(SOURCE_ITEM_KEY, Registries.ITEM.getId(chainItemSource).toString());
//        NbtList linksTag = new NbtList();
//
//        // Write complete links
//        for (ChainLink link : links) {
//            if (link.isDead()) continue;
//            if (link.getPrimary() != this) continue;
//            Entity secondary = link.getSecondary();
//            NbtCompound compoundTag = new NbtCompound();
//            compoundTag.putString(SOURCE_ITEM_KEY, Registries.ITEM.getId(link.getSourceItem()).toString());
//            if (secondary instanceof PlayerEntity) {
//                UUID uuid = secondary.getUuid();
//                compoundTag.putUuid("UUID", uuid);
//            } else if (secondary instanceof BlockAttachedEntity) {
//                BlockPos srcPos = this.attachedBlockPos;
//                BlockPos dstPos = ((BlockAttachedEntity) secondary).getAttachedBlockPos();
//                BlockPos relPos = dstPos.subtract(srcPos);
//                // Inverse rotation to store the position as 'facing' agnostic
//                Direction inverseFacing = Direction.fromRotation(Direction.SOUTH.asRotation() - getYaw());
//                relPos = getBlockPosAsFacingRelative(relPos, inverseFacing);
//                compoundTag.putInt("RelX", relPos.getX());
//                compoundTag.putInt("RelY", relPos.getY());
//                compoundTag.putInt("RelZ", relPos.getZ());
//            }
//            linksTag.add(compoundTag);
//        }
//
//        // Write old, incomplete links
//        linksTag.addAll(incompleteLinks);
//
//        if (!linksTag.isEmpty()) {
//            root.put("Chains", linksTag);
//        }
//    }
//
//    /**
//     * Read all the data from {@link #writeCustomDataToNbt(NbtCompound)}
//     * and stores the links in {@link #incompleteLinks}.
//     *
//     * @param root the tag to read from.
//     */
//    public void readCustomDataFromNbt(NbtCompound root) {
//        if (root.contains("Chains")) {
//            incompleteLinks.addAll(root.getList("Chains", NbtElement.COMPOUND_TYPE));
//        }
//        chainItemSource = Registries.ITEM.get(Identifier.tryParse(root.getString(SOURCE_ITEM_KEY)));
//    }
//
//    @Override
//    public void onBreak(ServerWorld world, @Nullable Entity breaker) {
//
//    }
//
//    /**
//     * Checks if the {@code distance} is within the {@link #VISIBLE_RANGE visible range}.
//     *
//     * @param distance the camera distance from the knot.
//     * @return true when it is in range.
//     */
//    @Environment(EnvType.CLIENT)
//    @Override
//    public boolean shouldRender(double distance) {
//        return true;
//    }
//
//    @Override
//    public Vec3d getLeashOffset() {
//        return new Vec3d(0, 4.5 / 16, 0);
//    }
//
//    /**
//     * The offset where a leash / chain will visually connect to.
//     */
//    @Environment(EnvType.CLIENT)
//    @Override
//    public Vec3d getLeashPos(float f) {
//        return getLerpedPos(f).add(0, 4.5 / 16, 0);
//    }
//
//    /**
//     * Interaction (attack or use) of a player and this entity.
//     * On the server it will:
//     * <ol>
//     * <li>Try to move existing link from player to this.</li>
//     * <li>Try to cancel chain links (when clicking a knot that already has a connection to {@code player}).</li>
//     * <li>Try to create a new connection.</li>
//     * <li>Try to destroy the knot with the item in the players hand.</li>
//     * </ol>
//     *
//     * @param player The player that interacted.
//     * @param hand   The hand that interacted.
//     * @return {@link ActionResult#SUCCESS} or {@link ActionResult#CONSUME} when the interaction was successful.
//     * @see #tryAttachHeldChains(PlayerEntity)
//     */
//    @Override
//    public ActionResult interact(PlayerEntity player, Hand hand) {
//        ItemStack handStack = player.getStackInHand(hand);
//        if (getWorld().isClient()) {
//            if (handStack.isIn(ModTagRegistry.CATENARY_ITEMS)) {
//                return ActionResult.SUCCESS;
//            }
//
//            if (ChainLinkEntity.canDestroyWith(handStack)) {
//                return ActionResult.SUCCESS;
//            }
//
//            return ActionResult.PASS;
//        }
//
//        // 1. Try to move existing link from player to this.
//        boolean madeConnection = tryAttachHeldChains(player);
//        if (madeConnection) {
//            onPlace();
//            return ActionResult.CONSUME;
//        }
//
//        // 2. Try to cancel chain links (when clicking same knot twice)
//        boolean broke = false;
//        for (ChainLink link : links) {
//            if (link.getSecondary() == player) {
//                broke = true;
//                link.destroy(true);
//            }
//        }
//        if (broke) {
//            return ActionResult.CONSUME;
//        }
//
//        // 3. Try to create a new connection
//        if (handStack.isIn(ModTagRegistry.CATENARY_ITEMS)) {
//            // Interacted with a valid chain item, create a new link
//            onPlace();
//            ChainLink.create(this, player, handStack.getItem());
//            // Allow changing the chainType of the knot
//            updateChainType(handStack.getItem());
//            if (!player.isCreative()) {
//                player.getStackInHand(hand).decrement(1);
//            }
//
//            return ActionResult.CONSUME;
//        }
//
//        // 4. Interacted with anything else, check for shears
//        if (ChainLinkEntity.canDestroyWith(handStack)) {
//            destroyLinks(!player.isCreative());
//            graceTicks = 0;
//            return ActionResult.CONSUME;
//        }
//
//        return ActionResult.PASS;
//    }
//
//    /**
//     * Destroys all chains held by {@code player} that are in range and creates new links to itself.
//     *
//     * @param player the player wo tries to make a connection.
//     * @return true if it has made a connection.
//     */
//    public boolean tryAttachHeldChains(PlayerEntity player) {
//        boolean hasMadeConnection = false;
//        List<ChainLink> attachableLinks = getHeldChainsInRange(player, getAttachedBlockPos());
//        for (ChainLink link : attachableLinks) {
//            // Prevent connections with self
//            if (link.getPrimary() == this) continue;
//
//            // Move that link to this knot
//            ChainLink newLink = ChainLink.create(link.getPrimary(), this, link.getSourceItem());
//
//            // Check if the link does not already exist
//            if (newLink != null) {
//                link.destroy(false);
//                link.removeSilently = true;
//                hasMadeConnection = true;
//            }
//        }
//        return hasMadeConnection;
//    }
//
//    public void onPlace() {
//        playSound(getSoundGroup().getPlaceSound(), 1.0F, 1.0F);
//    }
//
//    private BlockSoundGroup getSoundGroup() {
//        return ChainLink.getSoundGroup(chainItemSource);
//    }
//
//    /**
//     * Sets the chain type and sends a packet to the client.
//     *
//     * @param sourceItem The new chain type.
//     */
//    public void updateChainType(Item sourceItem) {
//        this.chainItemSource = sourceItem;
//
//        if (!getWorld().isClient()) {
//            Collection<ServerPlayerEntity> trackingPlayers = PlayerLookup.around((ServerWorld) getWorld(), getBlockPos(), ChainKnotEntity.VISIBLE_RANGE);
//            KnotChangePayload payload = new KnotChangePayload(getId(), sourceItem);
//            trackingPlayers.forEach(player -> ServerPlayNetworking.send(player, payload));
//        }
//    }
//
//    /**
//     * @return all complete links that are associated with this knot.
//     * @apiNote Operating on the list has potential for bugs as it does not include incomplete links.
//     * For example {@link ChainLink#create(ChainKnotEntity, Entity, Item)} checks if the link already exists
//     * using this list. Same goes for {@link #tryAttachHeldChains(PlayerEntity)}
//     * but at the end of the day it doesn't really matter.
//     * When an incomplete link is not resolved within the first two ticks it is unlikely to ever complete.
//     * And even if it completes it will be stopped either because the knot is dead or the duplicates check in {@code ChainLink}.
//     */
//    public List<ChainLink> getLinks() {
//        return links;
//    }
//
//    @Override
//    public SoundCategory getSoundCategory() {
//        return SoundCategory.BLOCKS;
//    }
//
//    /**
//     * Writes all client side relevant information into a {@link EntitySpawnS2CPacket} packet and sends it.
//     *
//     * @see PacketCreator
//     */
//    @Override
//    public Packet<ClientPlayPacketListener> createSpawnPacket(EntityTrackerEntry entityTrackerEntry) {
//        int id = Registries.ITEM.getRawId(chainItemSource);
//        return new EntitySpawnS2CPacket(this, id, this.getAttachedBlockPos());
//    }
//
//    @Override
//    public void onSpawnPacket(EntitySpawnS2CPacket packet) {
//        super.onSpawnPacket(packet);
//        int rawChainItemSourceId = packet.getEntityData();
//        chainItemSource = Registries.ITEM.get(rawChainItemSourceId);
//    }
//
//    /**
//     * Checks if the knot model of the knot entity should be rendered.
//     * To determine if the knot entity including chains should be rendered use {@link #shouldRender(double)}
//     *
//     * @return true if the knot is not attached to a wall.
//     */
//    @Environment(EnvType.CLIENT)
//    public boolean shouldRenderKnot() {
//        return attachTarget == null || !attachTarget.isIn(BlockTags.WALLS);
//    }
//
//    public void addLink(ChainLink link) {
//        links.add(link);
//    }
}
