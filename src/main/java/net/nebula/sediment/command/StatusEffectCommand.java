package net.nebula.sediment.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.nebula.sediment.StatusEffectRegistry;
import net.nebula.sediment.common.IStatusEffect;
import net.nebula.sediment.common.StatusContainer;

public final class StatusEffectCommand {

    private static final SuggestionProvider<CommandSourceStack> EFFECT_IDS =
            (context, builder) -> SharedSuggestionProvider.suggest(StatusEffectRegistry.ids(), builder);

    @FunctionalInterface
    private interface Action {
        boolean run(LivingEntity entity, String effectId);
    }

    private StatusEffectCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sediment")
                .requires(source -> source.hasPermission(3))
                .then(Commands.literal("apply")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests(EFFECT_IDS)
                                        .executes(context -> run(context, "applied", (entity, id) -> StatusContainer.apply(entity, id)))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(0))
                                                .executes(context -> run(context, "applied", (entity, id) -> StatusContainer.add(
                                                        entity, id, IntegerArgumentType.getInteger(context, "count"), 0)))
                                                .then(Commands.argument("potency", IntegerArgumentType.integer(0))
                                                        .executes(context -> run(context, "applied", (entity, id) -> StatusContainer.add(
                                                                entity,
                                                                id,
                                                                IntegerArgumentType.getInteger(context, "count"),
                                                                IntegerArgumentType.getInteger(context, "potency")))))))))
                .then(Commands.literal("addpotency")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests(EFFECT_IDS)
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(context -> run(context, "added potency to", (entity, id) -> StatusContainer.addPotency(
                                                        entity, id, IntegerArgumentType.getInteger(context, "amount"))))))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests(EFFECT_IDS)
                                        .then(Commands.argument("count", IntegerArgumentType.integer(0))
                                                .then(Commands.argument("potency", IntegerArgumentType.integer(0))
                                                        .executes(context -> run(context, "set", (entity, id) -> StatusContainer.set(
                                                                entity,
                                                                id,
                                                                IntegerArgumentType.getInteger(context, "count"),
                                                                IntegerArgumentType.getInteger(context, "potency")))))))))
                .then(Commands.literal("max")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests(EFFECT_IDS)
                                        .executes(context -> run(context, "maximized", (entity, id) -> StatusContainer.max(entity, id))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests(EFFECT_IDS)
                                        .executes(context -> run(context, "removed", (entity, id) -> StatusContainer.removeEffect(entity, id))))))
                .then(Commands.literal("expire")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .suggests(EFFECT_IDS)
                                        .executes(context -> run(context, "expired", (entity, id) -> StatusContainer.forceExpire(entity, id))))))
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(StatusEffectCommand::clear)))
                .then(Commands.literal("list")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(StatusEffectCommand::list)))
                .then(Commands.literal("registry")
                        .executes(StatusEffectCommand::registry)));
    }

    private static int run(CommandContext<CommandSourceStack> context, String verb, Action action) throws CommandSyntaxException {
        String effectId = StringArgumentType.getString(context, "effect");
        if (!StatusEffectRegistry.exists(effectId)) {
            context.getSource().sendFailure(Component.literal("Unknown status effect: " + effectId));
            return 0;
        }
        int done = 0;
        for (Entity entity : EntityArgument.getEntities(context, "targets")) {
            if (entity instanceof LivingEntity living && action.run(living, effectId)) {
                done++;
            }
        }
        if (done == 0) {
            context.getSource().sendFailure(Component.literal("Nothing changed for " + effectId));
            return 0;
        }
        final int total = done;
        context.getSource().sendSuccess(() -> Component.literal("Effect " + effectId + " " + verb + " on " + total + " entities"), true);
        return total;
    }

    private static int clear(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int done = 0;
        for (Entity entity : EntityArgument.getEntities(context, "targets")) {
            if (entity instanceof LivingEntity living && StatusContainer.clearAll(living)) {
                done++;
            }
        }
        if (done == 0) {
            context.getSource().sendFailure(Component.literal("No status effects to clear"));
            return 0;
        }
        final int total = done;
        context.getSource().sendSuccess(() -> Component.literal("Cleared status effects on " + total + " entities"), true);
        return total;
    }

    private static int list(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "target");
        StatusContainer container = StatusContainer.get(entity.getUUID());
        if (container == null) {
            context.getSource().sendSuccess(() -> Component.literal("No status effects on " + entity.getName().getString()), false);
            return 0;
        }
        Map<String, IStatusEffect> effects = container.getEffects();
        for (IStatusEffect effect : effects.values()) {
            String line = effect.getId()
                    + ": count " + effect.getCount().get() + "/" + effect.getCount().max()
                    + ", potency " + effect.getPotency().get() + "/" + effect.getPotency().max();
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return effects.size();
    }

    private static int registry(CommandContext<CommandSourceStack> context) {
        for (String id : StatusEffectRegistry.ids()) {
            String line = id + (StatusEffectRegistry.isBuiltIn(id) ? " (built-in)" : " (custom)");
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return StatusEffectRegistry.ids().size();
    }
}