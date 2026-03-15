package net.bteuk.network.utils;

import lombok.Getter;
import lombok.extern.java.Log;
import net.bteuk.network.sql.PlotSQL;
import teachingtutorials.utils.DBConnection;

/**
 * Represents a tutorial recommendation
 */
@Log
public class TutorialRecommendation {
    private final int iRecommendationID;

    private final int iPlotID;

    @Getter
    private final teachingtutorials.tutorialobjects.TutorialRecommendation linkedTutorialRecommendation;

    public TutorialRecommendation(DBConnection dbConnection, int iRecommendationID, int iPlotID) {
        this.iRecommendationID = iRecommendationID;
        this.iPlotID = iPlotID;
        this.linkedTutorialRecommendation = teachingtutorials.tutorialobjects.TutorialRecommendation.fetchTutorialRecommendationByID(dbConnection, log, iRecommendationID);
    }

    /**
     * Adds this tutorial recommendation to the plot database
     */
    public void addTutorialRecommendationToDB(PlotSQL plotSQL) {
        String sql = "INSERT INTO tutorial_recommendations (`plot_id`, `recommendation_id`) VALUES (" + this.iPlotID + ", " + this.iRecommendationID + ")";
        plotSQL.update(sql);
    }
}
