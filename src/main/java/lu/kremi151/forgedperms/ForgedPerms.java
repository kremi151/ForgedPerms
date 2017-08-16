package lu.kremi151.forgedperms;

import java.io.IOException;

import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.event.service.ChangeServiceProviderEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;

import com.google.inject.Inject;

import net.minecraftforge.server.permission.PermissionAPI;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

@Plugin(id = "forgedperms", name = "ForgedPerms", version = ForgedPerms.VERSION, authors = {"kremi151"}, description = "Linking the Forge permission API to the one of SpongeForge", dependencies = {@Dependency(id = "forge")})
public class ForgedPerms {
	
	public static final String VERSION = "1.0.0.2";
	
	@Inject
	private Logger logger;
	
	@Inject
	@DefaultConfig(sharedRoot = false)
	private ConfigurationLoader<CommentedConfigurationNode> config;
	
	PluginContainer container;
	private final ForgePermissionHandler handler = new ForgePermissionHandler(this, null);
	
	//Configs
	private boolean logHandlerSwitches = true;
	private boolean logStartup = true;

	@Listener(order = Order.LAST)
	public void preInit(GamePreInitializationEvent event) {
		try {
			ConfigurationNode node = config.load();
			this.logStartup = node.getNode("logging", "startup").getBoolean(true);
			this.logHandlerSwitches = node.getNode("logging", "switch").getBoolean(true);
		} catch (IOException e) {
			logger.error("Error loading configuration", e);
		}
		
		if(logStartup)logger.info("Fetching Sponge permission service and linking it to the Forge permission API...");
		this.container = Sponge.getPluginManager().fromInstance(this).orElseThrow(() -> new RuntimeException("Unexpected error"));

		PermissionAPI.setPermissionHandler(handler);
		Sponge.getGame().getServiceManager().getRegistration(PermissionService.class).ifPresent(reg -> switchHandler(reg.getProvider(), reg.getPlugin().getId()));
		
		Sponge.getGame().getCommandManager().register(this, CommandSpec.builder()
					.executor((src, args) -> {
						src.sendMessages(
								Text.of("ForgedPerms v" + VERSION), 
								Text.join(Text.of("Current used permission service: "), handler.getHandlerRepresentation().orElse(Text.of(TextColors.RED, "none")))
								);
						return CommandResult.success();
					})
					.permission("forgedperms")
					.description(Text.of("Provides the id of the plugin and the class name of the current used Sponge permission handler"))
					.build()
				, "forgedperms");
	}
	
	@Listener
	public void gameStopping(GameStoppingEvent event) {
		try {
			saveConfig();
		} catch (IOException e) {
			logger.error("Error saving configuration", e);
		}
	}
	
	@Listener
	public void reloadPlugin(GameReloadEvent event) {
		Sponge.getGame().getServiceManager().getRegistration(PermissionService.class).ifPresent(reg -> switchHandler(reg.getProvider(), reg.getPlugin().getId()));
	}
	
	private void saveConfig() throws IOException {
		CommentedConfigurationNode node = config.load();
		CommentedConfigurationNode loggingNode = node.getNode("logging")
				.setComment("Configuration entries for console logs");
		loggingNode.getNode("startup").setValue(this.logStartup).setComment("Log at plugin startup");
		loggingNode.getNode("switch").setValue(this.logHandlerSwitches).setComment("Log when permission handler switches");
		
		config.save(node);
	}
	
	@Listener
	public void serviceProviderChanged(ChangeServiceProviderEvent event) {
		if(event.getService() == PermissionService.class) {
			switchHandler((PermissionService) event.getNewProvider(), event.getNewProviderRegistration().getPlugin().getId());
		}
	}
	
	private void switchHandler(PermissionService service, String plugin_id) {
		this.handler.switchPermissionService(service, plugin_id);
		if(logHandlerSwitches)logger.info("Forge permission API has been linked to the permission service of class " + service.getClass().getName() + " from plugin " + plugin_id);
	}
	
}
