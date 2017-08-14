package lu.kremi151.forgedperms;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.service.ProviderRegistration;
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
	private final ForgedPerms plugin;
	
	ForgePermissionHandler(ForgedPerms plugin, @Nullable PermissionService perms){
		this.perms = perms;
		this.plugin = plugin;
	}
	
	void switchPermissionService(@Nonnull PermissionService perms) {
		if(perms == null)throw new NullPointerException("The supplied Sponge permission service must not be null");
		this.perms = perms;
	}
	
	Optional<Text> getHandlerRepresentation(){
		if(perms != null) {
			Optional<ProviderRegistration<PermissionService>> op = Sponge.getGame().getServiceManager().getRegistration(PermissionService.class);
			if(op.isPresent()) {
				if(op.get().getProvider() == perms) {
					return Optional.of(Text.of(TextColors.YELLOW, perms.getClass().getName(), TextColors.RESET, " (", TextColors.GOLD, op.get().getPlugin().getId(), TextColors.RESET, ")"));
				}else {
					//This should not happen normally
					return Optional.of(Text.of(TextColors.YELLOW, perms.getClass().getName(), TextColors.RED, "[Desynchronized, current permission service is " + op.get().getProvider().getClass() + " (" + op.get().getPlugin().getId() + ")]"));
				}
			}else {
				return Optional.of(Text.of(TextColors.YELLOW, perms.getClass().getName()));
			}
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
