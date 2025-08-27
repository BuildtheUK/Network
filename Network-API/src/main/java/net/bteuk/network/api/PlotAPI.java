package net.bteuk.network.api;

import java.util.List;

public interface PlotAPI {

    /**
     * Reverts plot submissions for the server.
     * - Reverts plots with status under review to status submitted.
     * - Reverts plots with status under verification to status awaiting verification.
     *
     * @param serverName the name of the server
     */
    void resetPlotSubmissions(String serverName);

    /**
     * Returns a list of active plots for the server.
     *
     * @param serverName the name of the server
     * @return a list of active plots by id
     */
    List<Integer> getActivePlots(String serverName);

    boolean createLocation(String locationName, String alias, String server, int coordMin, int coordMax, int xTransform, int yTransform);

    boolean createPlotRegion(String regionName, String server, String locationName);

    boolean setLocationAlias(String locationName, String alias);

    boolean setPlotDifficulty(int plotId, int difficulty);

    boolean clearZoneMembers(int zoneId);

    boolean setPlotStatus(int plotId, String status);

    boolean setZoneStatus(int zoneId, String status);

    boolean clearPlotMembers(int plotId);

    boolean setPlotSubmissionStatus(int plotId, String status);

    boolean removePlotSubmission(int plotId);

    boolean createPlotMember(int plotId, String uuid);

    boolean removePlotMember(int plotId, String uuid);

    boolean createZoneOwner(int zoneId, String uuid);

    boolean createZoneMember(int zoneId, String uuid);

    boolean removeZoneMember(int zoneId, String uuid);

    boolean setPlotLastEnter(int plotId, String uuid);

    boolean savePlotReviewCategoryFeedback(int reviewId, String category, String selection, int bookId);

    boolean saveBook(int bookId, int page, String content);

    boolean setPlotInactivityNotice(int plotId, String uuid);

    boolean createPlotCorner(int plotId, int cornerIndex, int x, int z);

    boolean createZoneCorner(int zoneId, int cornerIndex, int x, int z);

    int createPlot(int size, int difficulty, String locationName, int coordinateId);

    int createZone(String locationName, long expiration, boolean isPublic);

    int[][] getPlotCorners(int plotId);

    double getReviewerReputation(String uuid);

    boolean canReviewPlot(int plotId, String uuid, boolean isArchitect, boolean isReviewer);

    boolean canVerifyPlot(int plotId, String uuid, boolean isReviewer);

    int createReview(int plotId, String plotOwner, String reviewer, boolean accepted, boolean completed);

    int createVerification(int reviewId, String verifier, boolean acceptedOld, boolean acceptedNew);

    boolean savePlotVerificationCategory(int verificationId, String category, String selectionOld, String selectionNew, int bookIdOld, int bookIdNew);

    String getRegionLocation(String regionName);

    String getRegionServer(String regionName);

    int getXTransform(String location);

    int getZTransform(String location);

    boolean hasLocation(String location);

    String getPlotOwner(int plotID);

    String getZoneOwner(int zoneID);
}
