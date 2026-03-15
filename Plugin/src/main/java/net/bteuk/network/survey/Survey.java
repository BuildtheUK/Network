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
public class Survey {
    @Getter @Setter
    boolean isExisting;

    @Getter
    @Setter
    Timestamp SurveyCompleted;
    @Getter
    @Setter
    Timestamp SurveyLastEdited;
    @Getter
    @Setter
    boolean bFoundViaBTUK;
    @Getter
    @Setter
    boolean bFoundViaBTE;
    @Getter
    @Setter
    boolean bFoundViaBTUKExternal;
    @Getter
    @Setter
    boolean bFoundViaBTEExternal;
    @Getter
    @Setter
    boolean bFoundViaFriend;

    @Getter
    @Setter
    boolean bMediumTiktok;
    @Getter
    @Setter
    boolean bMediumYoutubeShorts;
    @Getter
    @Setter
    boolean bMediumYoutubeLongform;
    @Getter
    @Setter
    boolean bMediumInstagram;
    @Getter
    @Setter
    boolean bSearchEngineBrowsing;
    @Getter
    @Setter
    boolean bOnlineNews;
    @Getter
    @Setter
    boolean bTVNews;
    @Getter
    @Setter
    boolean bPhysicalNewspaper;

    @Getter
    @Setter
    boolean bSocialsTiktok;
    @Getter
    @Setter
    boolean bSocialsYoutubeShorts;
    @Getter
    @Setter
    boolean bSocialsYoutubeLongform;
    @Getter
    @Setter
    boolean bSocialsInstagram;
}
