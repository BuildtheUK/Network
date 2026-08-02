package net.bteuk.network.survey;

import lombok.Getter;
import org.btuk.network.lib.utils.ChatUtils;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.utils.NetworkUser;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.HashMap;
import java.util.Map;

public class SurveyBook {
    private final NetworkUser user;
    private final GlobalSQL globalSQL;
    @Getter
    private final Survey survey;
    private final Book[] books = new Book[5];
    /**
     * The current open page of the book, zero indexed
     */
    private int iCurrentPage;

    private static final Map<NetworkUser, SurveyBook> openSurveys = new HashMap<>();

    public SurveyBook(NetworkUser user, GlobalSQL globalSQL) {
        this.user = user;
        this.globalSQL = globalSQL;

        // Get survey details
        Survey existingSurvey = globalSQL.getSurveyOfUser(user.player.getUniqueId());
        this.survey = (existingSurvey == null) ? new Survey() : existingSurvey;

        iCurrentPage = 0;

        // Update survey with answers
        updateSurveyBooks();

        openSurveys.put(user, this);
    }

    /**
     * Opens a survey for a user
     */
    public static void openSurvey(NetworkUser user, GlobalSQL globalSQL) {
        SurveyBook openSurvey = SurveyBook.getOpenSurvey(user);
        if (openSurvey == null) {
            openSurvey = new SurveyBook(user, globalSQL);
        }
        openSurvey.openCurrentPage();
    }

    /**
     * Changes the open page dof the survey.
     *
     * @param iPage The page to change to, 1 indexed.
     */
    public void changePage(int iPage) {
        iCurrentPage = iPage - 1;
    }

    /**
     * Open page iPage of the survey.
     */
    public void openCurrentPage() {
        user.player.openBook(books[iCurrentPage]);
    }

    public void saveSurvey() {
        globalSQL.saveSurveyOfUser(user.player.getUniqueId(), survey);
        user.sendMessage(ChatUtils.success("Thank you! Survey has been saved!"));
        openSurveys.remove(user);
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
    public void updateSurveyBooks() {

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
        page1 = page1.appendNewline();
        page1 = page1.appendNewline();
        page1 = page1.appendNewline();
        page1 = page1.appendNewline();
        page1 = page1.appendNewline();
        page1 = appendPageChangeOption(page1, false, true, 1);

        this.books[0] = Book.book(Component.text(""), ChatUtils.line(user.player.getName()), page1);

        // Page 2 Question 1 answers
        Component page2 = Component.empty();
        page2 = appendOptionYesNo("Build the UK", page2, SurveyAnswerOption.Q1_BTUK, survey.bFoundViaBTUK, !survey.bFoundViaBTUK);
        page2 = page2.appendNewline(); // Extra line
        page2 = appendOptionYesNo("Build The Earth", page2, SurveyAnswerOption.Q1_BTE, survey.bFoundViaBTE, !survey.bFoundViaBTE);
        page2 = page2.appendNewline(); // Extra line
        page2 = appendOptionYesNo("External Media Mentioning Build the UK", page2, SurveyAnswerOption.Q1_BTUK_EXTERNAL, survey.bFoundViaBTUKExternal, !survey.bFoundViaBTUKExternal);
        page2 = page2.appendNewline(); // Extra line
        page2 = appendOptionYesNo("External Media Mentioning Build The Earth", page2, SurveyAnswerOption.Q1_BTE_EXTERNAL, survey.bFoundViaBTEExternal, !survey.bFoundViaBTEExternal);
        page2 = page2.appendNewline(); // Extra line
        page2 = appendOptionYesNo("From a Friend", page2, SurveyAnswerOption.Q1_FRIEND, survey.bFoundViaFriend, !survey.bFoundViaFriend);
        page2 = appendPageChangeOption(page2, true, true, 2);

        this.books[1] = Book.book(Component.text(""), ChatUtils.line(user.player.getName()), page2);

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
        page3 = page3.appendNewline();
        page3 = page3.appendNewline();
        page3 = page3.appendNewline();
        page3 = page3.appendNewline();
        page3 = page3.appendNewline();
        page3 = appendPageChangeOption(page3, true, true, 3);

        this.books[2] = Book.book(Component.text(""), ChatUtils.line(user.player.getName()), page3);

        // Page 4 Question 2 answers
        Component page4 = Component.empty();
        page4 = appendOptionYesNo("TikTok", page4, SurveyAnswerOption.Q2_TIKTOK, survey.bMediumTiktok, !survey.bMediumTiktok);
        page4 = appendOptionYesNo("YouTube Short", page4, SurveyAnswerOption.Q2_YT_SHORT, survey.bMediumYoutubeShorts, !survey.bMediumYoutubeShorts);
        page4 = appendOptionYesNo("YouTube Longform", page4, SurveyAnswerOption.Q2_YT_LONG, survey.bMediumYoutubeLongform, !survey.bMediumYoutubeLongform);
        page4 = appendOptionYesNo("YouTube Instagram", page4, SurveyAnswerOption.Q2_INSTAGRAM, survey.bMediumInstagram, !survey.bMediumInstagram);
        page4 = appendOptionYesNo("YouTube Search Engine - Browsing", page4, SurveyAnswerOption.Q2_SEARCH, survey.bSearchEngineBrowsing, !survey.bSearchEngineBrowsing);
        page4 = appendOptionYesNo("Online News", page4, SurveyAnswerOption.Q2_ONLINE_NEWS, survey.bOnlineNews, !survey.bOnlineNews);
        page4 = appendOptionYesNo("TV News", page4, SurveyAnswerOption.Q2_TV_NEWS, survey.bTVNews, !survey.bTVNews);
        page4 = appendOptionYesNo("Physical Newspaper", page4, SurveyAnswerOption.Q2_NEWSPAPER, survey.bPhysicalNewspaper, !survey.bPhysicalNewspaper);
        page4 = appendPageChangeOption(page4, true, true, 4);

        this.books[3] = Book.book(Component.text(""), ChatUtils.line(user.player.getName()), page4);

        // Page 5 Question 3 Which socials
        Component page5 = Component.empty();
        page5 = page5.append(Component.text("Question 3").decorate(TextDecoration.BOLD));
        page5 = page5.appendNewline();
        page5 = page5.append(Component.text("What social media platforms do you use regularly?"));
        page5 = page5.appendNewline();
        page5 = page5.appendNewline();
        page5 = appendOptionYesNo("TikTok", page5, SurveyAnswerOption.Q3_TIKTOK, survey.bSocialsTiktok, !survey.bSocialsTiktok);
        page5 = appendOptionYesNo("YouTube Shorts", page5, SurveyAnswerOption.Q3_YOUTUBE_SHORTS, survey.bSocialsYoutubeShorts, !survey.bSocialsYoutubeShorts);
        page5 = appendOptionYesNo("YouTube Longform", page5, SurveyAnswerOption.Q3_YOUTUBE_LONG, survey.bSocialsYoutubeLongform, !survey.bSocialsYoutubeLongform);
        page5 = appendOptionYesNo("Instagram", page5, SurveyAnswerOption.Q3_INSTAGRAM, survey.bSocialsInstagram, !survey.bSocialsInstagram);
        page5 = page5.appendNewline();
        page5 = page5.append(Component.text("Save Survey"));
        page5 = page5.appendSpace();
        page5 = page5.append(Component.text("[Save]")
                .color(TextColor.color(NamedTextColor.GREEN.value()))
                .clickEvent(ClickEvent.runCommand("/survey save")));
        page5 = page5.appendNewline();
        page5 = appendPageChangeOption(page5, true, false, 5);

        this.books[4] = Book.book(Component.text(""), ChatUtils.line(user.player.getName()), page5);
    }

    private Component appendOptionYesNo(String optionName, Component component, SurveyAnswerOption option, boolean bYSelected, boolean bNSelected) {
        component = component.append(Component.text(optionName));
        component = component.appendSpace();

        //[Y]
        component = component.append(Component.text("["));
        Component Y = Component.text("Y").color(TextColor.color(NamedTextColor.GREEN.value()))
                .clickEvent(ClickEvent.runCommand("/survey " + option.name() + " Y"));
        if (bYSelected) {
            component = component.append(Y.decorate(TextDecoration.BOLD).decorate(TextDecoration.UNDERLINED));
        }
        else {
            component = component.append(Y);
        }
        component = component.append(Component.text("]"));
        component = component.appendSpace();
        //[N]
        component = component.append(Component.text("["));
        Component N = Component.text("N").color(TextColor.color(NamedTextColor.RED.value()))
                .clickEvent(ClickEvent.runCommand("/survey " + option.name() + " N"));
        if (bNSelected) {
            component = component.append(N.decorate(TextDecoration.BOLD).decorate(TextDecoration.UNDERLINED));
        }
        else {
            component = component.append(N);
        }
        component = component.append(Component.text("]"));

        component = component.appendNewline();
        return component;
    }

    private Component appendPageChangeOption(Component component, boolean bIncludeBack, boolean bIncludeForwards, int iCurrentPage) {

        if (bIncludeBack) {
            component = component.append(Component.text("[Previous]").color(TextColor.color(NamedTextColor.LIGHT_PURPLE.value()))
                    .clickEvent(ClickEvent.runCommand("/survey " + SurveyAnswerOption.CHANGE_PAGE.name() + " " + (iCurrentPage - 1))));
            if (bIncludeForwards) {
                component = component.append(Component.text("        "));
                component = component.append(Component.text("[Next]").color(TextColor.color(NamedTextColor.LIGHT_PURPLE.value()))
                        .clickEvent(ClickEvent.runCommand("/survey " + SurveyAnswerOption.CHANGE_PAGE.name() + " " + (iCurrentPage + 1))));
            }
        } else if (bIncludeForwards) {
            component = component.append(Component.text("                     "));
            component = component.append(Component.text("[Next]").color(TextColor.color(NamedTextColor.LIGHT_PURPLE.value()))
                    .clickEvent(ClickEvent.runCommand("/survey " + SurveyAnswerOption.CHANGE_PAGE.name() + " " + (iCurrentPage + 1))));
        }
        return component;
    }
}
