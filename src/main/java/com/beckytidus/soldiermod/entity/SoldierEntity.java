package com.beckytidus.soldiermod.entity;

import com.beckytidus.soldiermod.SoldierMod;
import com.beckytidus.soldiermod.registry.SoldierNames;
import com.beckytidus.soldiermod.registry.SoldierSkins;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * A soldier. It is not a pet: it takes an {@link Order}, holds a {@link Formation}
 * slot in your squad, fights in a {@link SoldierClass} specialisation and earns a
 * {@link Rank} from the enemies it puts down. Every behaviour below is real -
 * it patrols its post, charges hostiles, falls back on command and calls out
 * contacts as it spots them.
 */
public class SoldierEntity extends PathfinderMob {
    private static final int REPATH_INTERVAL = 8;
    private static final int SCAN_INTERVAL = 12;
    private static final int GUARD_RADIUS = 14;
    private static final int PATROL_RADIUS = 10;
    private static final int CHARGE_RADIUS = 26;
    private static final double TELEPORT_DISTANCE = 48.0;
    private static final double SQUAD_COUNT_RANGE = 96.0;
    private static final int RETALIATE_TICKS = 120;

    private static final EntityDataAccessor<String> ORDER = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SOLDIER_CLASS = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> RANK = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SOLDIER_NAME = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> FORMATION = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> TEXTURE_VARIANT = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SQUAD_INDEX = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KILLS = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> ENGAGED = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<EntityReference<LivingEntity>>> OWNER = SynchedEntityData.defineId(SoldierEntity.class, EntityDataSerializers.OPTIONAL_LIVING_ENTITY_REFERENCE);

    private Order order = Order.FOLLOW;
    private SoldierClass soldierClass = SoldierClass.RIFLEMAN;
    private Rank rank = Rank.PRIVATE;
    private Formation formation = Formation.WEDGE;
    private int kills = 0;
    private int squadIndex = 0;
    private String soldierName = "";
    private String textureVariant = "";

    @Nullable
    private EntityReference<LivingEntity> ownerRef;
    @Nullable
    private BlockPos post;
    @Nullable
    private BlockPos patrolTarget;

    private int repathCooldown = 0;
    private int scanCooldown = 0;
    private int abilityCooldown = 0;
    private long lastSpeakTick = 0;
    private long lastHurtTick = -1000;
    private boolean engaged = false;
    private String lastTagName = "";

    public SoldierEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.setCanPickUpLoot(false);
    }

    public static AttributeSupplier.Builder createSoldierAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ORDER, Order.FOLLOW.name());
        builder.define(SOLDIER_CLASS, SoldierClass.RIFLEMAN.name());
        builder.define(RANK, Rank.PRIVATE.name());
        builder.define(SOLDIER_NAME, "");
        builder.define(FORMATION, Formation.WEDGE.name());
        builder.define(TEXTURE_VARIANT, "1");
        builder.define(SQUAD_INDEX, 0);
        builder.define(KILLS, 0);
        builder.define(ENGAGED, false);
        builder.define(OWNER, Optional.empty());
    }

    // ---------------------------------------------------------------- persistence

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        EntityReference.store(ownerRef, output, "Owner");
        output.putString("Order", order.name());
        output.putString("SoldierClass", soldierClass.name());
        output.putString("Rank", rank.name());
        output.putString("Formation", formation.name());
        output.putString("SoldierName", soldierName);
        output.putString("TextureVariant", textureVariant);
        output.putInt("Kills", kills);
        output.putInt("SquadIndex", squadIndex);
        if (post != null) {
            output.putInt("PostX", post.getX());
            output.putInt("PostY", post.getY());
            output.putInt("PostZ", post.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setOwnerReference(EntityReference.readWithOldOwnerConversion(input, "Owner", this.level()));
        order = Order.byName(input.getStringOr("Order", Order.FOLLOW.name()));
        soldierClass = SoldierClass.byName(input.getStringOr("SoldierClass", SoldierClass.RIFLEMAN.name()));
        rank = Rank.byName(input.getStringOr("Rank", Rank.PRIVATE.name()));
        formation = Formation.byName(input.getStringOr("Formation", Formation.WEDGE.name()));
        soldierName = input.getStringOr("SoldierName", "");
        textureVariant = input.getStringOr("TextureVariant", "");
        kills = input.getIntOr("Kills", 0);
        squadIndex = input.getIntOr("SquadIndex", 0);
        if (input.getIntOr("PostX", Integer.MIN_VALUE) != Integer.MIN_VALUE) {
            post = new BlockPos(input.getIntOr("PostX", 0), input.getIntOr("PostY", 0), input.getIntOr("PostZ", 0));
        }
        if (!level().isClientSide()) {
            syncTracked();
            applyClassAndRankModifiers();
        }
    }

    private void syncTracked() {
        entityData.set(ORDER, order.name());
        entityData.set(SOLDIER_CLASS, soldierClass.name());
        entityData.set(RANK, rank.name());
        entityData.set(SOLDIER_NAME, soldierName);
        entityData.set(FORMATION, formation.name());
        entityData.set(TEXTURE_VARIANT, textureVariant);
        entityData.set(SQUAD_INDEX, squadIndex);
        entityData.set(KILLS, kills);
        entityData.set(ENGAGED, engaged);
    }

    // ---------------------------------------------------------------- accessors

    @Nullable
    public Player getOwner() {
        return EntityReference.getLivingEntity(ownerRef, level()) instanceof Player p ? p : null;
    }

    @Nullable
    public UUID getOwnerUUID() {
        return entityData.get(OWNER).map(EntityReference::getUUID).orElse(null);
    }

    public boolean isOwnedBy(Player player) {
        UUID owner = getOwnerUUID();
        return owner != null && owner.equals(player.getUUID());
    }

    public void setOwner(@Nullable Player player) {
        setOwnerReference(player != null ? EntityReference.of((LivingEntity) player) : null);
        if (player != null) setPersistenceRequired();
    }

    private void setOwnerReference(@Nullable EntityReference<LivingEntity> ref) {
        this.ownerRef = ref;
        entityData.set(OWNER, Optional.ofNullable(ref));
    }

    public Order getOrder() {
        return level().isClientSide() ? Order.byName(entityData.get(ORDER)) : order;
    }

    public void setOrder(Order next) {
        setOrder(next, true);
    }

    public void setOrder(Order next, boolean announce) {
        this.order = next;
        this.patrolTarget = null;
        this.repathCooldown = 0;
        if (next.isAnchored()) this.post = blockPosition();
        if (next == Order.REGROUP || next == Order.RETREAT) this.setTarget(null);
        if (!level().isClientSide()) {
            entityData.set(ORDER, next.name());
            if (announce) {
                Player owner = getOwner();
                if (owner != null) {
                    owner.sendSystemMessage(Component.literal(prefix() + " " + next.label() + " - " + SoldierSpeech.acknowledge(getRandom())));
                }
            }
        }
    }

    public void setPost(@Nullable BlockPos pos) {
        this.post = pos;
    }

    @Nullable
    public BlockPos getPost() {
        return post;
    }

    public SoldierClass getSoldierClass() {
        return level().isClientSide() ? SoldierClass.byName(entityData.get(SOLDIER_CLASS)) : soldierClass;
    }

    public void setSoldierClass(SoldierClass next) {
        this.soldierClass = next;
        if (!level().isClientSide()) {
            entityData.set(SOLDIER_CLASS, next.name());
            applyClassAndRankModifiers();
        }
    }

    public Rank getRank() {
        return level().isClientSide() ? Rank.byName(entityData.get(RANK)) : rank;
    }

    public int getKills() {
        return level().isClientSide() ? entityData.get(KILLS) : kills;
    }

    public Formation getFormation() {
        return level().isClientSide() ? Formation.byName(entityData.get(FORMATION)) : formation;
    }

    public void setFormation(Formation next) {
        this.formation = next;
        if (!level().isClientSide()) entityData.set(FORMATION, next.name());
    }

    public int getSquadIndex() {
        return level().isClientSide() ? entityData.get(SQUAD_INDEX) : squadIndex;
    }

    public void setSquadIndex(int index) {
        this.squadIndex = Math.max(0, index);
        if (!level().isClientSide()) entityData.set(SQUAD_INDEX, squadIndex);
    }

    public boolean isEngaged() {
        return level().isClientSide() ? entityData.get(ENGAGED) : engaged;
    }

    private void setEngaged(boolean value) {
        if (this.engaged == value) return;
        this.engaged = value;
        if (!level().isClientSide()) entityData.set(ENGAGED, value);
    }

    public String getSoldierName() {
        if (level().isClientSide()) {
            String synced = entityData.get(SOLDIER_NAME);
            return synced != null && !synced.isEmpty() ? synced : "Soldier";
        }
        return soldierName != null && !soldierName.isEmpty() ? soldierName : "Soldier";
    }

    public void setSoldierName(String name) {
        this.soldierName = name != null ? name : "";
        if (!level().isClientSide()) entityData.set(SOLDIER_NAME, this.soldierName);
        updateNameTag();
    }

    /** Callsign with rank chevrons, no class - for the HUD and the name tag. */
    public String getNameWithChevrons() {
        String chevrons = getRank().chevrons();
        return chevrons.isEmpty() ? getSoldierName() : getSoldierName() + " " + chevrons;
    }

    public String getTextureVariant() {
        return level().isClientSide() ? entityData.get(TEXTURE_VARIANT) : textureVariant;
    }

    public void setTextureVariant(String variant) {
        this.textureVariant = variant != null ? variant : "";
        if (!level().isClientSide()) entityData.set(TEXTURE_VARIANT, this.textureVariant);
    }

    /** Name shown on the tag: callsign, chevrons and class. */
    public String getTagName() {
        return getSoldierName() + " " + getRank().chevrons() + " [" + getSoldierClass().label() + "]";
    }

    // ---------------------------------------------------------------- attributes

    /** Applies the class and rank bonuses as attribute modifiers. */
    public void applyClassAndRankModifiers() {
        if (level().isClientSide()) return;
        double health = soldierClass.bonusHealth() + rank.bonusHealth();
        double damage = soldierClass.bonusDamage() + rank.bonusDamage();
        double speed = soldierClass.speedMultiplier() - 1.0;
        double armor = soldierClass.bonusArmor();
        double kb = soldierClass.bonusKnockbackResistance();
        double range = soldierClass.bonusFollowRange();

        applyModifier(Attributes.MAX_HEALTH, "class_health", health, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(Attributes.ATTACK_DAMAGE, "class_damage", damage, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(Attributes.MOVEMENT_SPEED, "class_speed", speed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        applyModifier(Attributes.ARMOR, "class_armor", armor, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(Attributes.KNOCKBACK_RESISTANCE, "class_knockback", kb, AttributeModifier.Operation.ADD_VALUE);
        applyModifier(Attributes.FOLLOW_RANGE, "class_range", range, AttributeModifier.Operation.ADD_VALUE);

        if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
    }

    private void applyModifier(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
                               String path, double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = getAttribute(attribute);
        if (instance == null) return;
        Identifier id = Identifier.fromNamespaceAndPath(SoldierMod.MOD_ID, path);
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    // ---------------------------------------------------------------- tick

    @Override
    public void tick() {
        if (!level().isClientSide()) {
            Player owner = getOwner();
            if (owner != null) setPersistenceRequired();
            if (owner != null && order != Order.HOLD) {
                double distSq = owner.distanceToSqr(this);
                if (distSq > TELEPORT_DISTANCE * TELEPORT_DISTANCE) {
                    snapTo(owner.getX(), owner.getY(), owner.getZ(), getYRot(), getXRot());
                }
            }
        }
        super.tick();
        if (level().isClientSide()) return;

        if (soldierName.isEmpty()) setSoldierName(SoldierNames.pick(getRandom()));
        if (textureVariant.isEmpty()) setTextureVariant(SoldierSkins.pick(getRandom()));

        if (repathCooldown > 0) repathCooldown--;
        if (scanCooldown > 0) scanCooldown--;
        if (abilityCooldown > 0) abilityCooldown--;

        tickTargeting();
        tickOrder();
        tickFieldAbility();
        updateNameTag();
    }

    /** Aggressive orders seek their own targets; the rest only fight back. */
    private void tickTargeting() {
        LivingEntity target = getTarget();
        if (target != null && !target.isAlive()) {
            setTarget(null);
            target = null;
        }

        if (order.isAggressive()) {
            if (target != null) {
                // Anchored orders give up the chase when the enemy leaves the post.
                if (order.isAnchored() && post != null && target.distanceToSqr(
                        post.getX() + 0.5, post.getY() + 0.5, post.getZ() + 0.5) > (double) GUARD_RADIUS * GUARD_RADIUS * 1.5) {
                    setTarget(null);
                }
            } else if (scanCooldown <= 0) {
                Monster found = findNearestEnemy();
                if (found != null) {
                    setTarget(found);
                    if (now() - lastSpeakTick > 200 && getRandom().nextFloat() < 0.25f) {
                        speak(SoldierSpeech.engaging(getRandom()));
                    }
                }
                scanCooldown = SCAN_INTERVAL;
            }
        } else if (target != null) {
            boolean mayRetaliate = order != Order.RETREAT && order != Order.REGROUP
                    && now() - lastHurtTick < RETALIATE_TICKS;
            if (!mayRetaliate) setTarget(null);
        }

        setEngaged(getTarget() != null && getTarget().isAlive());
    }

    @Nullable
    private Monster findNearestEnemy() {
        double radius = order == Order.CHARGE ? CHARGE_RADIUS : GUARD_RADIUS;
        double x = getX();
        double y = getY();
        double z = getZ();
        if (order.isAnchored() && post != null) {
            x = post.getX() + 0.5;
            y = post.getY() + 0.5;
            z = post.getZ() + 0.5;
        }
        var box = new net.minecraft.world.phys.AABB(x - radius, y - radius, z - radius, x + radius, y + radius, z + radius);
        List<Monster> monsters = level().getEntitiesOfClass(Monster.class, box, m -> m.isAlive() && !m.isInvulnerable());
        Monster best = null;
        double bestDist = Double.MAX_VALUE;
        for (Monster monster : monsters) {
            double d = monster.distanceToSqr(x, y, z);
            if (d < bestDist) {
                bestDist = d;
                best = monster;
            }
        }
        return best;
    }

    private void tickOrder() {
        Player owner = getOwner();
        if (owner == null) {
            getNavigation().stop();
            setEngaged(false);
            return;
        }

        switch (order) {
            case FOLLOW -> { followOwner(owner); }
            case HOLD -> { hold(owner); }
            case GUARD -> { guard(owner); }
            case PATROL -> { patrol(owner); }
            case CHARGE -> { charge(owner); }
            case REGROUP -> { regroup(owner); }
            case RETREAT -> { retreat(owner); }
        }
    }

    // ---------------------------------------------------------------- orders

    private void followOwner(Player owner) {
        int size = squadSize(owner);
        double[] offset = formation.offset(getSquadIndex(), size);
        double[] goal = formationPoint(owner, offset[0], offset[1]);
        double distSq = distanceToSqr(goal[0], goal[1], goal[2]);
        if (distSq > 2.2 * 2.2) {
            if (repathCooldown <= 0) {
                getNavigation().moveTo(goal[0], goal[1], goal[2], distSq > 12 * 12 ? 1.3 : 1.0);
                setSprinting(distSq > 12 * 12);
                repathCooldown = REPATH_INTERVAL;
            }
        } else {
            getNavigation().stop();
            setSprinting(false);
            getLookControl().setLookAt(owner, 10.0F, getMaxHeadXRot());
        }
    }

    private void hold(Player owner) {
        if (post == null) post = blockPosition();
        if (getTarget() != null && getTarget().isAlive()) return;
        goToPost(0.9);
        getLookControl().setLookAt(owner, 6.0F, getMaxHeadXRot());
    }

    private void guard(Player owner) {
        if (post == null) post = blockPosition();
        if (getTarget() != null && getTarget().isAlive()) return;
        goToPost(1.0);
    }

    private void patrol(Player owner) {
        if (post == null) post = blockPosition();
        if (getTarget() != null && getTarget().isAlive()) return;
        if (patrolTarget == null || distanceToSqr(patrolTarget.getX() + 0.5, patrolTarget.getY(), patrolTarget.getZ() + 0.5) < 2.5 * 2.5) {
            patrolTarget = pickPatrolPoint();
            repathCooldown = 0;
        }
        if (patrolTarget != null && repathCooldown <= 0) {
            getNavigation().moveTo(patrolTarget.getX() + 0.5, patrolTarget.getY(), patrolTarget.getZ() + 0.5, 0.8);
            repathCooldown = REPATH_INTERVAL * 2;
        }
    }

    private void charge(Player owner) {
        if (getTarget() != null && getTarget().isAlive()) return;
        // Nothing to fight: close up on the commander and hold the line.
        double distSq = distanceToSqr(owner);
        if (distSq > 10 * 10) {
            if (repathCooldown <= 0) {
                getNavigation().moveTo(owner, 1.15);
                repathCooldown = REPATH_INTERVAL;
            }
        } else {
            getNavigation().stop();
            getLookControl().setLookAt(owner, 8.0F, getMaxHeadXRot());
        }
    }

    private void regroup(Player owner) {
        setTarget(null);
        double distSq = distanceToSqr(owner);
        if (distSq > 6 * 6) {
            if (repathCooldown <= 0) {
                getNavigation().moveTo(owner, 1.2);
                repathCooldown = REPATH_INTERVAL;
            }
        } else {
            getNavigation().stop();
            getLookControl().setLookAt(owner, 10.0F, getMaxHeadXRot());
        }
    }

    private void retreat(Player owner) {
        setTarget(null);
        setSprinting(true);
        double distSq = distanceToSqr(owner);
        if (distSq > 5 * 5) {
            if (repathCooldown <= 0) {
                getNavigation().moveTo(owner, 1.35);
                repathCooldown = REPATH_INTERVAL;
            }
        } else {
            getNavigation().stop();
            setSprinting(false);
            getLookControl().setLookAt(owner, 10.0F, getMaxHeadXRot());
        }
    }

    private void goToPost(double speed) {
        if (post == null) return;
        double distSq = distanceToSqr(post.getX() + 0.5, post.getY(), post.getZ() + 0.5);
        if (distSq > 3 * 3) {
            if (repathCooldown <= 0) {
                getNavigation().moveTo(post.getX() + 0.5, post.getY(), post.getZ() + 0.5, speed);
                repathCooldown = REPATH_INTERVAL;
            }
        } else {
            getNavigation().stop();
            // Sweep the horizon while standing post.
            if (getRandom().nextInt(60) == 0) {
                setYRot(getYRot() + getRandom().nextFloat() * 60 - 30);
            }
        }
    }

    @Nullable
    private BlockPos pickPatrolPoint() {
        if (post == null) return null;
        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = getRandom().nextIntBetweenInclusive(-PATROL_RADIUS, PATROL_RADIUS);
            int dz = getRandom().nextIntBetweenInclusive(-PATROL_RADIUS, PATROL_RADIUS);
            if (dx * dx + dz * dz < 9) continue;
            BlockPos candidate = post.offset(dx, 0, dz);
            // Walk to whatever the ground level is around the post.
            for (int dy = 3; dy >= -4; dy--) {
                BlockPos ground = candidate.offset(0, dy, 0);
                if (level().getBlockState(ground).isAir() && level().getBlockState(ground.below()).isSolidRender()) {
                    return ground;
                }
            }
        }
        return post;
    }

    /** Squad slot index: the nearest soldiers take the front slots. */
    public int squadSize(Player owner) {
        List<SoldierEntity> squad = owner.level().getEntitiesOfClass(SoldierEntity.class,
                owner.getBoundingBox().inflate(SQUAD_COUNT_RANGE), s -> s.isOwnedBy(owner) && s.isAlive());
        return Math.max(1, squad.size());
    }

    /** World position for a formation offset relative to the commander's facing. */
    private static double[] formationPoint(Player owner, double sideways, double behind) {
        double yaw = Math.toRadians(owner.getYRot());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);
        double x = owner.getX() + rightX * sideways - forwardX * behind;
        double z = owner.getZ() + rightZ * sideways - forwardZ * behind;
        return new double[]{x, owner.getY(), z};
    }

    // ---------------------------------------------------------------- field abilities

    private void tickFieldAbility() {
        if (!soldierClass.hasFieldAbility() || abilityCooldown > 0) return;
        switch (soldierClass) {
            case MEDIC -> {
                medicAid();
                abilityCooldown = 60;
            }
            case SCOUT -> {
                scoutMark();
                abilityCooldown = 40;
            }
            default -> abilityCooldown = 40;
        }
    }

    /** The medic patches up itself, its commander and any squad member nearby. */
    private void medicAid() {
        if (!(level() instanceof ServerLevel sl)) return;
        boolean didSomething = false;
        if (getHealth() < getMaxHealth()) {
            heal(2.0f);
            didSomething = true;
        }
        Player owner = getOwner();
        if (owner != null && distanceToSqr(owner) < 8 * 8 && owner.getHealth() < owner.getMaxHealth()) {
            owner.heal(2.0f);
            didSomething = true;
        }
        for (SoldierEntity mate : sl.getEntitiesOfClass(SoldierEntity.class, getBoundingBox().inflate(8.0),
                s -> s != this && s.isAlive())) {
            if (mate.getHealth() < mate.getMaxHealth()) {
                mate.heal(2.0f);
                didSomething = true;
            }
        }
        if (didSomething) {
            sl.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.6, getZ(), 3, 0.4, 0.4, 0.4, 0.0);
        }
    }

    /** The scout paints the nearest hostile so the whole squad can see it. */
    private void scoutMark() {
        Monster enemy = findNearestEnemy();
        if (enemy == null) return;
        enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false));
    }

    // ---------------------------------------------------------------- combat

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean hit = super.doHurtTarget(level, target);
        if (hit && target instanceof LivingEntity living && !living.isAlive()) {
            registerKill();
        }
        return hit;
    }

    private void registerKill() {
        kills++;
        if (!level().isClientSide()) entityData.set(KILLS, kills);
        Rank earned = Rank.forKills(kills);
        if (earned != rank) {
            rank = earned;
            if (!level().isClientSide()) {
                entityData.set(RANK, rank.name());
                applyClassAndRankModifiers();
            }
            Player owner = getOwner();
            if (owner != null) {
                owner.sendSystemMessage(Component.literal(prefix() + " Promoted to " + rank.label() + "! " + rank.chevrons()));
            }
            speak(SoldierSpeech.kill(getRandom()));
        } else if (now() - lastSpeakTick > 160 && getRandom().nextFloat() < 0.3f) {
            speak(SoldierSpeech.kill(getRandom()));
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        Player owner = getOwner();
        if (owner != null && source.getEntity() == owner) {
            return false; // a soldier does not fight its own commander
        }
        lastHurtTick = now();
        if (getRandom().nextFloat() < 0.2f) speak(SoldierSpeech.hurt(getRandom()));
        boolean result = super.hurtServer(level, source, amount);
        if (this.getHealth() <= 0 && owner != null) {
            owner.sendSystemMessage(Component.literal(prefix() + " " + SoldierSpeech.lastStand(getRandom())));
        }
        return result;
    }

    // ---------------------------------------------------------------- interaction

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        return com.beckytidus.soldiermod.interaction.SoldierInteractionHandler.handle(player, this);
    }

    public void equipWeapon(ItemStack stack) {
        if (!(level() instanceof ServerLevel sl)) return;
        ItemStack old = getMainHandItem();
        setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
        if (!old.isEmpty()) spawnAtLocation(sl, old);
    }

    // ---------------------------------------------------------------- misc

    public void speak(String message) {
        Player owner = getOwner();
        if (owner == null) return;
        lastSpeakTick = now();
        owner.sendSystemMessage(Component.literal(prefix() + " " + message));
    }

    public void reportTo(ServerPlayer player, boolean actionBar) {
        player.sendSystemMessage(getStatusText(), actionBar);
    }

    private String prefix() {
        return "\u2694 " + getSoldierName() + ":";
    }

    public Component getStatusText() {
        String state = isEngaged() ? " \u2694 ENGAGED" : "";
        return Component.literal("\u2694 " + getTagName()
                + "  \u2022 " + getOrder().label()
                + "  \u2022 Kills " + getKills()
                + "  \u2022 " + (int) getHealth() + "/" + (int) getMaxHealth() + " HP"
                + state);
    }

    public String getStatusString() {
        return getStatusText().getString();
    }

    public String getShortStatus() {
        return getOrder().label();
    }

    private void updateNameTag() {
        String tag = getTagName();
        if (tag.equals(lastTagName)) return;
        lastTagName = tag;
        this.setCustomName(Component.literal(tag));
    }

    private long now() {
        return level() instanceof ServerLevel sl ? sl.getGameTime() : 0L;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return getOwner() == null && !isPersistenceRequired();
    }

    @Override
    public boolean wantsToPickUp(ServerLevel level, ItemStack stack) {
        return false;
    }

    /** Soldiers never target their commander or each other. */
    @Override
    public boolean canAttack(LivingEntity target) {
        if (target instanceof Player p && isOwnedBy(p)) return false;
        if (target instanceof SoldierEntity s) {
            Player owner = getOwner();
            if (owner != null && s.isOwnedBy(owner)) return false;
        }
        return super.canAttack(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof Player p && isOwnedBy(p)) return;
        super.setTarget(target);
    }
}
