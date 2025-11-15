package net.bteuk.network.api;

import net.bteuk.network.api.plotsystem.PlotStatus;
import net.bteuk.network.api.plotsystem.ReviewCategory;
import net.bteuk.network.api.plotsystem.ReviewSelection;
import net.bteuk.network.api.plotsystem.SubmittedStatus;

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

    List<Integer> getActivePlotsForLocation(String location);

    boolean createLocation(String locationName, String alias, String server, int coordMin, int coordMax, int xTransform, int yTransform);

    boolean createPlotRegion(String regionName, String server, String locationName);

    boolean setLocationAlias(String locationName, String alias);

    String getLocationAlias(String locationName);

    boolean setPlotDifficulty(int plotId, int difficulty);

    boolean clearZoneMembers(int zoneId);

    boolean setPlotStatus(int plotId, String status);

    boolean setZoneStatus(int zoneId, String status);

    boolean clearPlotMembers(int plotId);

    boolean setPlotSubmissionStatus(int plotId, String status);

    SubmittedStatus getPlotSubmissionStatus(int plotId);

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

    String getPlotOwner(int plotId);

    String getZoneOwner(int zoneID);

    boolean isPlotOwner(int plotId, String uuid);

    boolean isPlotMember(int plotId, String uuid);

    boolean isPlotClaimed(int plotId);

    int getNumberOfPlots(String uuid);

    String getPlotLocation(int plotId);

    String getZoneLocation(int zoneId);

    String getLocationServer(String location);

    PlotStatus getPlotStatus(int plotId);

    int getPlotDifficulty(int plotId);

    int getPlotSize(int plotId);

    boolean isPlotUnclaimed(int plotId);

    boolean createPlotOwner(int plotId, String uuid);

    boolean hasPlotOwnerOrMember(int plotId);

    boolean isZonePublic(int zoneId);

    int getPlotCoordinate(int plotId);

    void updatePlotCoordinate(int plotId, int coordinateId);

    boolean isZoneOwner(String uuid);

    int getNumberOfZones();

    boolean plotExists(int plotId);

    List<String> getBookPages(int bookId);

    void updatePlotCategoryFeedback(int reviewId, String category, String selection, int bookId);

    int getHighestBookId();

    int getLocationCoordMin(String location);

    int getLocationCoordMax(String location);

    List<String> getLocationRegions(String location);

    boolean locationExists(String location);

    void deleteLocation(String location);

    void deleteRegionsForLocation(String location);

    void updateLastSubmit(String uuid, long time);

    long getLastSubmit(String uuid);

    void createPlotSubmission(int plotId);

    int getDeniedPlotCount(int plotId, String uuid);

    String getPlotReviewer(int plotId, String uuid, int attempt);

    String getPlotReviewer(int reviewId);

    int getReviewId(int plotId, String uuid, int attempt);

    int getActiveReviewId(int plotId);

    List<ReviewCategory> getReviewCategories(int reviewId);

    ReviewSelection getReviewSelection(int reviewId, ReviewCategory category);

    int getReviewBookId(int reviewId, ReviewCategory category);

    List<ReviewCategory> getVerificationCategories(int verificationId);

    ReviewSelection getVerificationSelectionOld(int verificationId, ReviewCategory category);

    int getVerificationBookIdOld(int verificationId, ReviewCategory category);

    ReviewSelection getVerificationSelectionNew(int verificationId, ReviewCategory category);

    int getVerificationBookIdNew(int verificationId, ReviewCategory category);

    void completeReview(int reviewId, boolean accepted);

    void updateReviewerReputation(String uuid, double reputation);

    boolean getReviewOutcome(int reviewId);
}
