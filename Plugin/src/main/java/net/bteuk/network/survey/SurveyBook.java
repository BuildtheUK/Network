package net.bteuk.network.survey;

import lombok.Getter;
import net.bteuk.network.Network;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.NetworkUser;
import net.bteuk.network.utils.Utils;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

public class SurveyBook implements Listener {
    private final Network network;
    private final NetworkUser user;
    private final GlobalSQL globalSQL;
    @Getter
    private final Survey survey;
    // private final ItemStack bookItem;
    // private final UUID bookUuid;
    private Book book;

    private static final HashMap<NetworkUser, SurveyBook> openSurveys = new HashMap<>();

    public SurveyBook(Network network, NetworkUser user, GlobalSQL globalSQL) {
        this.network = network;
        this.user = user;
        this.globalSQL = globalSQL;

        // Get survey details
        Survey existingSurvey = globalSQL.getSurveyOfUser(user.player.getUniqueId());
        this.survey = (existingSurvey == null) ? new Survey() : existingSurvey;

        // Update survey with answers
        updateSurveyBook();

        // //Create book item
        // this.bookUuid = UUID.randomUUID();
        // bookItem = new ItemStack(Material.WRITTEN_BOOK);
        // BookMeta bookItemMeta = (BookMeta) bookItem;
        // bookItemMeta.setAuthor(bookUuid.toString());
        // bookItem.setItemMeta(bookItemMeta);
        //
        // Utils.giveItem(network, user.player, bookItem, "UK Survey");
        Bukkit.getServer().getPluginManager().registerEvents(this, network);
        openSurveys.put(user, this);
    }

    /**
     * Opens a survey for a user
     *
     * @param network
     * @param user
     * @param globalSQL
     */
    public static void openSurvey(Network network, NetworkUser user, GlobalSQL globalSQL) {
        SurveyBook openSurvey = SurveyBook.getOpenSurvey(user);
        if (openSurvey == null)
            openSurvey = new SurveyBook(network, user, globalSQL);
        openSurvey.open();
    }

    /**
     * Open the review book.
     */
    private void open() {
        user.player.openBook(book);
    }

    public void unregister() {
        openSurveys.remove(user);
        HandlerList.unregisterAll(this);
        user.player.teleport(user.player.getLocation().add(0.0001, 0, 0));
    }

    /**
     * @param user The user whom to get the open survey of.
     * @return The open survey of the given user, or null if the user has no open survey.
     */
    public static SurveyBook getOpenSurvey(NetworkUser user) {
        return openSurveys.get(user);
    }

    /**
     * Update book based on survey responses
     */
    private void updateSurveyBook() {

        // Page 1 - Which outlet
        Component page1 = Component.empty();
        // Question 1
        page1 = page1.append(Component.text("Question 1").decorate(TextDecoration.BOLD));
        page1 = page1.appendNewline();
        page1 = page1.appendNewline();
        page1 = page1.append(Component.text("Prior to joining the server, who did you find out about BTUK (Formerly BTE UK) from?"));
        page1 = page1.appendNewline();
        page1 = page1.appendNewline();
        page1 = page1.append(Component.text("Answer on next page !"));

        // Page 2 Question 1 answers
        Component page2 = Component.empty();
        page2 = appendOptionYesNo("Build the UK", page2, AnswerOption.Q1_BTUK, survey.bFoundViaBTUK, !survey.bFoundViaBTUK);
        page2 = page2.appendNewline(); // Extra line
        page2 = appendOptionYesNo("Build The Earth", page2, AnswerOption.Q1_BTE, survey.bFoundViaBTE, !survey.bFoundViaBTE);
        page2 = page2.appendNewline(); // Extra line
        page2 = appendOptionYesNo("External Media Mentioning Build the UK", page2, AnswerOption.Q1_BTUKExternal, survey.bFoundViaBTUKExternal, !survey.bFoundViaBTUKExternal);
        page2 = page2.appendNewline(); // Extra line
        page2 = appendOptionYesNo("External Media Mentioning Build The Earth", page2, AnswerOption.Q1_BTEExternal, survey.bFoundViaBTEExternal, !survey.bFoundViaBTEExternal);
        page2 = page2.appendNewline(); // Extra line
        page2 = appendOptionYesNo("From a Friend", page2, AnswerOption.Q1_Friend, survey.bFoundViaFriend, !survey.bFoundViaFriend);
        page2 = page2.appendNewline(); // Extra line

        // Page 3 Question 2 - Which medium
        Component page3 = Component.empty();
        // Question 2
        page3 = page3.append(Component.text("Question 2").decorate(TextDecoration.BOLD));
        page3 = page3.appendNewline();
        page3 = page3.appendNewline();
        page3 = page3.append(Component.text("Prior to joining, through which mediums did you hear about Build the UK or Build The Earth?"));
        page3 = page3.appendNewline();
        page3 = page3.appendNewline();
        page3 = page3.append(Component.text("Answer on next page !"));

        // Page 4 Question 2 answers
        Component page4 = Component.empty();
        page4 = appendOptionYesNo("TikTok", page4, AnswerOption.Q2_TikTok, survey.bMediumTiktok, !survey.bMediumTiktok);
        page4 = appendOptionYesNo("YouTube Short", page4, AnswerOption.Q2_YTShort, survey.bMediumYoutubeShorts, !survey.bMediumYoutubeShorts);
        page4 = appendOptionYesNo("YouTube Longform", page4, AnswerOption.Q2_YTLong, survey.bMediumYoutubeLongform, !survey.bMediumYoutubeLongform);
        page4 = appendOptionYesNo("YouTube Instagram", page4, AnswerOption.Q2_Instagram, survey.bMediumInstagram, !survey.bMediumInstagram);
        page4 = appendOptionYesNo("YouTube Search Engine - Browsing", page4, AnswerOption.Q2_Search, survey.bSearchEngineBrowsing, !survey.bSearchEngineBrowsing);
        page4 = appendOptionYesNo("Online News", page4, AnswerOption.Q2_OnlineNews, survey.bOnlineNews, !survey.bOnlineNews);
        page4 = appendOptionYesNo("TV News", page4, AnswerOption.Q2_TVNews, survey.bTVNews, !survey.bTVNews);
        page4 = appendOptionYesNo("Physical Newspaper", page4, AnswerOption.Q2_Newspaper, survey.bPhysicalNewspaper, !survey.bPhysicalNewspaper);

        // Page 5 Question 3 Which socials
        Component page5 = Component.empty();
        page5 = page5.append(Component.text("Question 3").decorate(TextDecoration.BOLD));
        page5 = page5.appendNewline();
        page5 = page5.appendNewline();
        page5 = page5.append(Component.text("What social media platforms do you use regularly?"));
        page5 = page5.appendNewline();
        page5 = page5.appendNewline();
        page5 = appendOptionYesNo("TikTok", page5, AnswerOption.Q3_TikTok, survey.bSocialsTiktok, !survey.bSocialsTiktok);
        page5 = appendOptionYesNo("YouTube", page5, AnswerOption.Q3_YouTube, survey.bSocialsYoutubeShorts, !survey.bSocialsYoutubeShorts);
        page5 = appendOptionYesNo("Instagram", page5, AnswerOption.Q3_Instagram, survey.bSocialsInstagram, !survey.bSocialsInstagram);
        page5 = page5.appendNewline();
        page5 = page5.append(Component.text("Save Survey"));
        page5 = page5.appendSpace();
        page5 = page5.append(Component.text("[Save]")
                .color(TextColor.color(NamedTextColor.GREEN.value()))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/survey save")));

        this.book = Book.book(Component.text(""), ChatUtils.line(user.player.getName()), page1, page2, page3, page4, page5);
    }

    private Component appendOptionYesNo(String optionName, Component component, AnswerOption option, boolean bBoldY, boolean bBoldN) {
        component = component.append(Component.text(optionName));
        component = component.appendSpace();

        //[Y]
        component = component.append(Component.text("["));
        Component Y = Component.text("Y").color(TextColor.color(NamedTextColor.GREEN.value()))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/survey " + option.name() + " Y"));
        if (bBoldY)
            component = component.append(Y.decorate(TextDecoration.BOLD));
        else
            component = component.append(Y);
        component = component.append(Component.text("]"));
        component = component.appendSpace();
        //[N]
        component = component.append(Component.text("["));
        Component N = Component.text("N").color(TextColor.color(NamedTextColor.RED.value()))
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/survey " + option.name() + " N"));
        if (bBoldN)
            component = component.append(N.decorate(TextDecoration.BOLD));
        else
            component = component.append(N);
        component = component.append(Component.text("]"));

        component = component.appendNewline();
        return component;
    }

    public void saveSurvey() {
        globalSQL.saveSurveyOfUser(user.player.getUniqueId(), survey);
    }

    public enum AnswerOption {
        Q1_BTUK,
        Q1_BTE,
        Q1_BTUKExternal,
        Q1_BTEExternal,
        Q1_Friend,
        Q2_TikTok,
        Q2_YTShort,
        Q2_YTLong,
        Q2_Instagram,
        Q2_Search,
        Q2_OnlineNews,
        Q2_TVNews,
        Q2_Newspaper,
        Q3_TikTok,
        Q3_YouTube,
        Q3_Instagram
    }

    // @EventHandler
    // public void interactEvent(PlayerInteractEvent event) {
    //     if (!event.getPlayer().getUniqueId().equals(user.player.getUniqueId()))
    //         return;
    //     if (event.getItem().getItemMeta() == null)
    //         return;
    //     if (event.getItem().getItemMeta() instanceof BookMeta bookMeta) {
    //         if (bookMeta.getAuthor().equals(this.bookUuid.toString())) {
    //             this.open();
    //             event.setCancelled(true);
    //         }
    //     }
    // }
    //
    // @EventHandler
    // public void inventoryClickEvent(InventoryClickEvent event) {
    //     if (!event.getWhoClicked().getUniqueId().equals(user.player.getUniqueId()))
    //         return;
    //     if (Objects.requireNonNull(event.getCurrentItem()).getItemMeta() == null)
    //         return;
    //     if (event.getCurrentItem().getItemMeta() instanceof BookMeta bookMeta) {
    //         if (bookMeta.getAuthor().equals(this.bookUuid.toString())) {
    //             this.open();
    //             event.setCancelled(true);
    //         }
    //     }
    // }

}
