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
import net.bteuk.network.survey.SurveyBook;
import net.bteuk.network.utils.NetworkUser;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

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
            SurveyBook.openSurvey(instance, user, globalSQL);
        } else if (args.length == 1) {
            if (args[0].equalsIgnoreCase("save")) {
                SurveyBook surveyBook = SurveyBook.getOpenSurvey(user);
                if (surveyBook != null) {
                    surveyBook.saveSurvey();
                    surveyBook.unregister();
                }
            }
        } else if (args.length == 2) {
            SurveyBook surveyBook = SurveyBook.getOpenSurvey(user);
            if (surveyBook == null)
                return;

            SurveyBook.AnswerOption answerOption = SurveyBook.AnswerOption.valueOf(args[0].toUpperCase());
            boolean bYes = args[1].equals("Y");
            switch (answerOption) {
                case Q1_BTUK -> surveyBook.getSurvey().setBFoundViaBTUK(bYes);
                case Q1_BTE -> surveyBook.getSurvey().setBFoundViaBTE(bYes);
                case Q1_BTUKExternal -> surveyBook.getSurvey().setBFoundViaBTUKExternal(bYes);
                case Q1_BTEExternal -> surveyBook.getSurvey().setBFoundViaBTEExternal(bYes);
                case Q1_Friend -> surveyBook.getSurvey().setBFoundViaFriend(bYes);
                case Q2_TikTok -> surveyBook.getSurvey().setBMediumTiktok(bYes);
                case Q2_YTShort -> surveyBook.getSurvey().setBMediumYoutubeShorts(bYes);
                case Q2_YTLong -> surveyBook.getSurvey().setBMediumYoutubeLongform(bYes);
                case Q2_Instagram -> surveyBook.getSurvey().setBMediumInstagram(bYes);
                case Q2_Search -> surveyBook.getSurvey().setBSearchEngineBrowsing(bYes);
                case Q2_OnlineNews -> surveyBook.getSurvey().setBOnlineNews(bYes);
                case Q2_TVNews -> surveyBook.getSurvey().setBTVNews(bYes);
                case Q2_Newspaper -> surveyBook.getSurvey().setBPhysicalNewspaper(bYes);
                case Q3_TikTok -> surveyBook.getSurvey().setBSocialsTiktok(bYes);
                case Q3_YouTube -> surveyBook.getSurvey().setBSocialsYoutubeShorts(bYes);
                case Q3_Instagram -> surveyBook.getSurvey().setBSocialsInstagram(bYes);
            }
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
