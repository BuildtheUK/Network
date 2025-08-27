package net.bteuk.network.services;

import net.bteuk.network.CustomChat;
import net.bteuk.network.utils.Role;
import net.bteuk.network.utils.Roles;
import net.bteuk.teachingtutorials.services.PromotionService;
import org.bukkit.entity.Player;

public class NetworkPromotionService implements PromotionService {

    private final Roles roles;
    private final CustomChat chat;

    public NetworkPromotionService(Roles roles, CustomChat chat) {
        this.roles = roles;
        this.chat = chat;
    }

    @Override
    public void promote(Player player) {
        // If the builder role is default, promote the user.
        Role currentRole = roles.builderRole(player);

        if (currentRole != null && currentRole.getId().equals("default")) {
            roles.alterRole(player.getUniqueId().toString(), player.getName(), "applicant", false, true, chat).join();
            roles.alterRole(player.getUniqueId().toString(), player.getName(), "default", false, true, chat).join();
        }
    }

    @Override
    public String getDescription() {
        return "Network promotion service.";
    }
}
