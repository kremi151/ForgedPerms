package lu.kremi151.forgedperms;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.spongepowered.api.service.permission.PermissionDescription;
import org.spongepowered.api.service.permission.PermissionService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.serializer.TextSerializers;

import com.mojang.authlib.GameProfile;

import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.IPermissionHandler;
import net.minecraftforge.server.permission.context.IContext;

public class ForgePermissionHandler implements IPermissionHandler{
	
	private PermissionService perms;
	private String plugin_id;
	private final ForgedPerms plugin;
	
	ForgePermissionHandler(ForgedPerms plugin, @Nullable PermissionService perms){
		this.perms = perms;
		this.plugin = plugin;
	}
	
	void switchPermissionService(@Nonnull PermissionService perms, @Nonnull String plugin_id) {
		if(perms == null)throw new NullPointerException("The supplied Sponge permission service must not be null");
		if(plugin_id == null)throw new NullPointerException("The supplied provider plugin id must not be null");
		this.perms = perms;
		this.plugin_id = plugin_id;
	}
	
	Optional<Text> getHandlerRepresentation(){
		if(perms != null) {
			return Optional.of(Text.of(TextColors.YELLOW, perms.getClass().getName(), TextColors.RESET, " (", TextColors.GOLD, plugin_id, TextColors.RESET, ")"));
		}else {
			return Optional.empty();
		}
	}

	@Override
	public void registerNode(String node, DefaultPermissionLevel level, String desc) {
		if(perms != null)perms.newDescriptionBuilder(plugin).ifPresent(builder -> builder.id(node).description(Text.of(desc)).register());
	}

	@Override
	public Collection<String> getRegisteredNodes() {
		if(perms != null) {
			return perms.getDescriptions().stream().map(desc -> desc.getId()).collect(Collectors.toCollection(() -> new LinkedList<>()));
		}else {
			return Collections.emptySet();
		}
	}

	@Override
	public boolean hasPermission(GameProfile profile, String node, IContext context) {
		if(perms != null) {
			return perms.getUserSubjects().get(profile.getId().toString()).hasPermission(node);
		}else {
			return false;
		}
	}

	@Override
	public String getNodeDescription(String node) {
		if(perms != null) {
			Optional<PermissionDescription> desc = perms.getDescription(node);
			if(desc.isPresent()) {
				return TextSerializers.FORMATTING_CODE.serialize(desc.get().getDescription());
			}else {
				return "";
			}
		}else {
			return "";
		}
		
	}

}
