package gc.grivyzom.gZSociety.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import gc.grivyzom.gZSociety.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class SocietyAdminCommand {

    public static BrigadierCommand create(Main plugin) {
        LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder.<CommandSource>literal("gzsociety")
                .requires(source -> source.hasPermission("gzsociety.admin"))
                // Base command /gzsociety
                .executes(ctx -> {
                    ctx.getSource().sendMessage(Component.text("--- GZ-Society Admin ---", NamedTextColor.GOLD));
                    ctx.getSource().sendMessage(
                            Component.text("/gzsociety reload - Reloads config and language.", NamedTextColor.GRAY));
                    ctx.getSource().sendMessage(
                            Component.text("/gzsociety status - Shows plugin status.", NamedTextColor.GRAY));
                    return Command.SINGLE_SUCCESS;
                })
                // Subcommand /gzsociety reload
                .then(LiteralArgumentBuilder.<CommandSource>literal("reload")
                        .requires(source -> source.hasPermission("gzsociety.admin.reload"))
                        .executes(ctx -> {
                            try {
                                // Reload config
                                plugin.getConfigManager().load();

                                // Reload language with new setting
                                String newLanguage = plugin.getConfigManager().getLanguage();
                                plugin.getLanguageManager().load(newLanguage);

                                ctx.getSource().sendMessage(Component.text("✓ ", NamedTextColor.GREEN)
                                        .append(Component.text("Configuration and language reloaded!",
                                                NamedTextColor.WHITE)));
                                ctx.getSource().sendMessage(Component.text("  Language: ", NamedTextColor.GRAY)
                                        .append(Component.text(newLanguage, NamedTextColor.AQUA)));

                                plugin.getLogger().info("Config and language reloaded by {}",
                                        ctx.getSource().toString());
                            } catch (Exception e) {
                                plugin.getLogger().error("Failed to reload", e);
                                ctx.getSource().sendMessage(
                                        Component.text("✗ Failed to reload. Check console.", NamedTextColor.RED));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                // Subcommand /gzsociety status
                .then(LiteralArgumentBuilder.<CommandSource>literal("status")
                        .requires(source -> source.hasPermission("gzsociety.admin.status"))
                        .executes(ctx -> {
                            String storageType = plugin.getConfigManager().getStorageType();
                            String language = plugin.getLanguageManager().getCurrentLanguage();
                            int cachedPlayers = plugin.getPlayerManager().getLoadedPlayers().size();

                            ctx.getSource()
                                    .sendMessage(Component.text("--- GZ-Society Status ---", NamedTextColor.AQUA));
                            ctx.getSource().sendMessage(Component.text("  Version: ", NamedTextColor.GRAY)
                                    .append(Component.text("1.2.0-SNAPSHOT", NamedTextColor.GREEN)));
                            ctx.getSource().sendMessage(Component.text("  Language: ", NamedTextColor.GRAY)
                                    .append(Component.text(language, NamedTextColor.GOLD)));
                            ctx.getSource().sendMessage(Component.text("  Storage: ", NamedTextColor.GRAY)
                                    .append(Component.text(storageType, NamedTextColor.GOLD)));
                            ctx.getSource().sendMessage(Component.text("  Cached Players: ", NamedTextColor.GRAY)
                                    .append(Component.text(String.valueOf(cachedPlayers), NamedTextColor.GOLD)));

                            return Command.SINGLE_SUCCESS;
                        }));

        LiteralCommandNode<CommandSource> node = builder.build();
        BrigadierCommand command = new BrigadierCommand(node);

        // Register aliases
        plugin.getServer().getCommandManager().register(
                new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("gzs").redirect(node).build()));
        plugin.getServer().getCommandManager().register(
                new BrigadierCommand(
                        LiteralArgumentBuilder.<CommandSource>literal("societyadmin").redirect(node).build()));
        plugin.getServer().getCommandManager().register(
                new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("sa").redirect(node).build()));

        return command;
    }
}
