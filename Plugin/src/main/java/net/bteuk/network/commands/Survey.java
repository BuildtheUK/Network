package net.bteuk.network.commands;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import lombok.extern.java.Log;
import net.bteuk.network.CustomChat;
import net.bteuk.network.Network;
import net.bteuk.network.core.Time;
import net.bteuk.network.lib.dto.UserUpdate;
import net.bteuk.network.lib.utils.ChatUtils;
import net.bteuk.network.socket.MessageSender;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.survey.SurveyAnswerOption;
import net.bteuk.network.survey.SurveyBook;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.logging.Level;

@Log
public class Survey extends AbstractCommand {

    private final Network instance;
    private final GlobalSQL globalSQL;

    public Survey(Network instance, GlobalSQL globalSQL) {
        this.instance = instance;
        this.globalSQL = globalSQL;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, String @NonNull [] args) {

        // Check if the sender is a player.
        Player player = getPlayer(stack);
        if (player == null) {
            return;
        }

        NetworkUser user = instance.getUser(player);

        // If u is null, cancel.
        if (user == null) {
            log.severe("User " + player.getName() + " can not be found!");
            player.sendMessage(ChatUtils.error("User can not be found, please relog!"));
            return;
        }

        if (args.length == 0) {
            SurveyBook.openSurvey(user, globalSQL);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("save")) {
                SurveyBook surveyBook = SurveyBook.getOpenSurvey(user);
                if (surveyBook != null) {
                    surveyBook.saveSurvey();
                }
        } else if (args.length == 2) {
            SurveyBook surveyBook = SurveyBook.getOpenSurvey(user);
            if (surveyBook == null)
                return;

            SurveyAnswerOption answerOption;

            try {
                 answerOption = SurveyAnswerOption.valueOf(args[0].toUpperCase());
            }
            catch (IllegalArgumentException e) {
                log.log(Level.WARNING, "Invalid answer option at command survey: " + args[0], e);
                return;
            }

            boolean bYes = args[1].equals("Y");
            switch (answerOption) {
                case Q1_BTUK -> surveyBook.getSurvey().setBFoundViaBTUK(bYes);
                case Q1_BTE -> surveyBook.getSurvey().setBFoundViaBTE(bYes);
                case Q1_BTUK_EXTERNAL -> surveyBook.getSurvey().setBFoundViaBTUKExternal(bYes);
                case Q1_BTE_EXTERNAL -> surveyBook.getSurvey().setBFoundViaBTEExternal(bYes);
                case Q1_FRIEND -> surveyBook.getSurvey().setBFoundViaFriend(bYes);
                case Q2_TIKTOK -> surveyBook.getSurvey().setBMediumTiktok(bYes);
                case Q2_YT_SHORT -> surveyBook.getSurvey().setBMediumYoutubeShorts(bYes);
                case Q2_YT_LONG -> surveyBook.getSurvey().setBMediumYoutubeLongform(bYes);
                case Q2_INSTAGRAM -> surveyBook.getSurvey().setBMediumInstagram(bYes);
                case Q2_SEARCH -> surveyBook.getSurvey().setBSearchEngineBrowsing(bYes);
                case Q2_ONLINE_NEWS -> surveyBook.getSurvey().setBOnlineNews(bYes);
                case Q2_TV_NEWS -> surveyBook.getSurvey().setBTVNews(bYes);
                case Q2_NEWSPAPER -> surveyBook.getSurvey().setBPhysicalNewspaper(bYes);
                case Q3_TIKTOK -> surveyBook.getSurvey().setBSocialsTiktok(bYes);
                case Q3_YOUTUBE_SHORTS -> surveyBook.getSurvey().setBSocialsYoutubeShorts(bYes);
                case Q3_YOUTUBE_LONG -> surveyBook.getSurvey().setBSocialsYoutubeLongform(bYes);
                case Q3_INSTAGRAM -> surveyBook.getSurvey().setBSocialsInstagram(bYes);
                case CHANGE_PAGE -> {
                    try {
                        int toPage = Integer.parseInt(args[1]);
                        surveyBook.changePage(toPage);
                    }
                    catch (NumberFormatException e) {
                        log.log(Level.WARNING, "Invalid page number: " + args[1], e);
                    }
                }
            }
            surveyBook.updateSurveyBooks();
            surveyBook.openCurrentPage();
        }
    }

    @Override
    public String getLabel() {
        return "survey";
    }

    @Override
    public String getDescription() {
        return "Complete the UK Survey";
    }
}
