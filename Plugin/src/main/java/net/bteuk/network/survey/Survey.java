package net.bteuk.network.survey;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * Stores survey answers
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Survey {
    private boolean isExisting;

    Timestamp SurveyCompleted;
    Timestamp SurveyLastEdited;
    boolean bFoundViaBTUK;
    boolean bFoundViaBTE;
    boolean bFoundViaBTUKExternal;
    boolean bFoundViaBTEExternal;
    boolean bFoundViaFriend;

    boolean bMediumTiktok;
    boolean bMediumYoutubeShorts;
    boolean bMediumYoutubeLongform;
    boolean bMediumInstagram;
    boolean bSearchEngineBrowsing;
    boolean bOnlineNews;
    boolean bTVNews;
    boolean bPhysicalNewspaper;

    boolean bSocialsTiktok;
    boolean bSocialsYoutubeShorts;
    boolean bSocialsYoutubeLongform;
    boolean bSocialsInstagram;
}
