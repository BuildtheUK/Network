package net.bteuk.network.api.impl;

import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.api.plotsystem.PlotStatus;
import net.bteuk.network.api.plotsystem.ReviewCategory;
import net.bteuk.network.api.plotsystem.ReviewSelection;
import net.bteuk.network.api.plotsystem.SubmittedStatus;
import net.bteuk.network.core.Time;
import net.bteuk.network.sql.GlobalSQL;
import net.bteuk.network.sql.PlotSQL;

import java.util.ArrayList;
import java.util.List;

public class PlotAPIImpl implements PlotAPI {

    private final PlotSQL plotSQL;

    private final GlobalSQL globalSQL;

    public PlotAPIImpl(PlotSQL plotSQL, GlobalSQL globalSQL) {
        this.plotSQL = plotSQL;
        this.globalSQL = globalSQL;
    }

    @Override
    public void resetPlotSubmissions(String serverName) {
        plotSQL.update(
                "UPDATE plot_submission AS ps INNER JOIN plot_data AS pd ON ps.plot_id=pd.id SET ps.status='submitted' WHERE ps.status='under review' AND pd.location IN (SELECT " +
                        "name FROM location_data WHERE server='" + serverName + "');");
        plotSQL.update(
                "UPDATE plot_submission AS ps INNER JOIN plot_data AS pd ON ps.plot_id=pd.id SET ps.status='awaiting verification' WHERE ps.status='under verification' AND pd" +
                        ".location IN (SELECT name FROM location_data WHERE server='" + serverName + "');");
    }

    @Override
    public List<Integer> getActivePlots(String serverName) {
        return plotSQL.getIntList(
                "SELECT pd.id FROM plot_data AS pd INNER JOIN location_data AS ld ON ld.name=pd.location WHERE pd.status IN ('unclaimed','claimed','submitted') AND " + "ld" +
                        ".server='" + serverName + "';");
    }

    @Override
    public List<Integer> getActivePlotsForLocation(String location) {
        return plotSQL.getIntList("SELECT pd.id FROM plot_data AS pd WHERE pd.location='" + location + "' AND pd.status IN ('unclaimed','claimed','submitted');");
    }

    @Override
    public boolean createLocation(String locationName, String alias, String server, int coordMin, int coordMax, int xTransform, int zTransform) {
        return plotSQL.createLocation(locationName, alias, server, coordMin, coordMax, xTransform, zTransform);
    }

    @Override
    public boolean createPlotRegion(String regionName, String server, String locationName) {
        return plotSQL.createRegion(regionName, server, locationName);
    }

    @Override
    public boolean setLocationAlias(String locationName, String alias) {
        return plotSQL.updateLocationAlias(locationName, alias);
    }

    @Override
    public String getLocationAlias(String locationName) {
        return plotSQL.getString("SELECT alias FROM location_data WHERE name='" + locationName + "';");
    }

    @Override
    public boolean setPlotDifficulty(int plotId, int difficulty) {
        return plotSQL.update("UPDATE plot_data SET difficulty=" + difficulty + " WHERE id=" + plotId + ";");
    }

    @Override
    public boolean clearZoneMembers(int zoneId) {
        return plotSQL.update("DELETE FROM zone_members WHERE id=" + zoneId + ";");
    }

    @Override
    public boolean setPlotStatus(int plotId, String status) {
        return plotSQL.update("UPDATE plot_data SET status='" + status + "' WHERE id=" + plotId + ";");
    }

    @Override
    public boolean setZoneStatus(int zoneId, String status) {
        return plotSQL.update("UPDATE zones SET status='" + status + "' WHERE id=" + zoneId + ";");
    }

    @Override
    public boolean clearPlotMembers(int plotId) {
        return plotSQL.update("DELETE FROM plot_members WHERE id=" + plotId + ";");
    }

    @Override
    public boolean setPlotSubmissionStatus(int plotId, String status) {
        return plotSQL.update("UPDATE plot_submission SET status='" + status + "' WHERE plot_id=" + plotId + ";");
    }

    @Override
    public SubmittedStatus getPlotSubmissionStatus(int plotId) {
        return SubmittedStatus.fromDatabaseValue(plotSQL.getString("SELECT status FROM plot_submission WHERE id=" + plotId + ";"));
    }

    @Override
    public boolean removePlotSubmission(int plotId) {
        return plotSQL.update("DELETE FROM plot_submission WHERE plot_id=" + plotId + ";");
    }

    @Override
    public boolean createPlotMember(int plotId, String uuid) {
        return plotSQL.update("INSERT INTO plot_members(id, uuid, is_owner) VALUES(" + plotId + ", '" + uuid + "', 0);");
    }

    @Override
    public boolean removePlotMember(int plotId, String uuid) {
        return plotSQL.update("DELETE FROM plot_members WHERE id=" + plotId + " AND uuid='" + uuid + "';");
    }

    @Override
    public boolean createZoneOwner(int zoneId, String uuid) {
        return plotSQL.update("INSERT INTO zone_members(id, uuid, is_owner) VALUES(" + zoneId + ", '" + uuid + "', 1);");
    }

    @Override
    public boolean createZoneMember(int zoneId, String uuid) {
        return plotSQL.update("INSERT INTO zone_members(id, uuid, is_owner) VALUES(" + zoneId + ", '" + uuid + "', 0);");
    }

    @Override
    public boolean removeZoneMember(int zoneId, String uuid) {
        return plotSQL.update("DELETE FROM zone_members WHERE id=" + zoneId + " AND uuid='" + uuid + "';");
    }

    @Override
    public boolean setPlotLastEnter(int plotId, String uuid) {
        return plotSQL.update("UPDATE plot_members SET last_enter='" + Time.currentTime() + "' WHERE id=" + plotId + " AND uuid='" + uuid + "';");
    }

    @Override
    public boolean savePlotReviewCategoryFeedback(int reviewId, String category, String selection, int bookId) {
        return plotSQL.savePlotReviewCategoryFeedback(reviewId, category, selection, bookId);
    }

    @Override
    public boolean saveBook(int bookId, int page, String content) {
        return plotSQL.saveBook(bookId, page, content);
    }

    @Override
    public boolean setPlotInactivityNotice(int plotId, String uuid) {
        return plotSQL.update("UPDATE plot_members SET inactivity_notice=1 WHERE id=" + plotId + " AND uuid='" + uuid + "';");
    }

    @Override
    public boolean createPlotCorner(int plotId, int cornerIndex, int x, int z) {
        return plotSQL.update("INSERT INTO plot_corners(id, corner, x, z) VALUES(" + plotId + ", " + cornerIndex + ", " + x + ", " + z + ");");
    }

    @Override
    public boolean createZoneCorner(int zoneId, int cornerIndex, int x, int z) {
        return plotSQL.update("INSERT INTO zone_corners(id, corner, x, z) VALUES(" + zoneId + ", " + cornerIndex + ", " + x + ", " + z + ");");
    }

    @Override
    public int createPlot(int size, int difficulty, String locationName, int coordinateId) {
        return plotSQL.createPlot(size, difficulty, locationName, coordinateId);
    }

    @Override
    public int createZone(String locationName, long expiration, boolean isPublic) {
        return plotSQL.createZone(locationName, expiration, isPublic);
    }

    @Override
    public int[][] getPlotCorners(int plotId) {
        return plotSQL.getPlotCorners(plotId);
    }

    @Override
    public double getReviewerReputation(String uuid) {
        return plotSQL.getReviewerReputation(uuid);
    }

    @Override
    public boolean canReviewPlot(int plotId, String uuid, boolean isArchitect, boolean isReviewer) {
        return plotSQL.canReviewPlot(plotId, uuid, isArchitect, isReviewer);
    }

    @Override
    public boolean canVerifyPlot(int plotId, String uuid, boolean isReviewer) {
        return plotSQL.canVerifyPlot(plotId, uuid, isReviewer);
    }

    @Override
    public int createReview(int plotId, String plotOwner, String reviewer, boolean accepted, boolean completed) {
        return plotSQL.createReview(plotId, plotOwner, reviewer, accepted, completed);
    }

    @Override
    public int createVerification(int reviewId, String verifier, boolean acceptedOld, boolean acceptedNew) {
        return plotSQL.createVerification(reviewId, verifier, acceptedOld, acceptedNew);
    }

    @Override
    public boolean savePlotVerificationCategory(int verificationId, String category, String selectionOld, String selectionNew, int bookIdOld, int bookIdNew) {
        return plotSQL.savePlotVerificationCategory(verificationId, category, selectionOld, selectionNew, bookIdOld, bookIdNew);
    }

    @Override
    public String getRegionLocation(String regionName) {
        return plotSQL.getString("SELECT location FROM regions WHERE region='" + regionName + "';");
    }

    @Override
    public String getRegionServer(String regionName) {
        return plotSQL.getString("SELECT server FROM regions WHERE region='" + regionName + "';");
    }

    @Override
    public int getXTransform(String location) {
        return plotSQL.getInt("SELECT xTransform FROM location_data WHERE name='" + location + "';");

    }

    @Override
    public int getZTransform(String location) {
        return plotSQL.getInt("SELECT zTransform FROM location_data WHERE name='" + location + "';");
    }

    @Override
    public boolean hasLocation(String location) {
        return plotSQL.hasRow("SELECT name FROM location_data WHERE name='" + location + "';");
    }

    @Override
    public String getPlotOwner(int plotId) {
        return plotSQL.getString("SELECT uuid FROM plot_members WHERE id=" + plotId + " AND is_owner=1;");
    }

    @Override
    public String getZoneOwner(int zoneID) {
        return plotSQL.getString("SELECT uuid FROM zone_members WHERE id=" + zoneID + " AND is_owner=1;");
    }

    @Override
    public boolean isPlotOwner(int plotId, String uuid) {
        return plotSQL.hasRow("SELECT id FROM plot_members WHERE id=" + plotId + " AND uuid='" + uuid + "' AND is_owner=1;");
    }

    @Override
    public boolean isPlotMember(int plotId, String uuid) {
        return plotSQL.hasRow("SELECT id FROM plot_members WHERE id=" + plotId + " AND uuid='" + uuid + "' AND is_owner=0;");

    }

    @Override
    public boolean isPlotClaimed(int plotId) {
        return plotSQL.hasRow("SELECT id FROM plot_data WHERE id=" + plotId + " AND status='claimed';");
    }

    @Override
    public int getNumberOfPlots(String uuid) {
        return plotSQL.getInt("SELECT count(id) FROM plot_members WHERE uuid='" + uuid + "';");
    }

    @Override
    public String getPlotLocation(int plotId) {
        return plotSQL.getString("SELECT location FROM plot_data WHERE id=" + plotId + ";");
    }

    @Override
    public String getZoneLocation(int zoneId) {
        return plotSQL.getString("SELECT location FROM zones WHERE id=" + zoneId + ";");
    }

    @Override
    public String getLocationServer(String location) {
        return plotSQL.getString("SELECT server FROM location_data WHERE name='" + location + "';");
    }

    @Override
    public PlotStatus getPlotStatus(int plotId) {
        return PlotStatus.fromDatabaseValue(plotSQL.getString("SELECT status FROM plot_data WHERE id=" + plotId + ";"));
    }

    @Override
    public int getPlotDifficulty(int plotId) {
        return plotSQL.getInt("SELECT difficulty FROM plot_data WHERE id=" + plotId + ";");
    }

    @Override
    public int getPlotSize(int plotId) {
        return plotSQL.getInt("SELECT size FROM plot_data WHERE id=" + plotId + ";");
    }

    @Override
    public boolean isPlotUnclaimed(int plotId) {
        return plotSQL.hasRow("SELECT id FROM plot_data WHERE id=" + plotId + " AND status='unclaimed';");
    }

    @Override
    public boolean createPlotOwner(int plotId, String uuid) {
        return plotSQL.update("INSERT INTO plot_members(id, uuid, is_owner, last_enter) VALUES(" + plotId + ", '" + uuid + "', 1, " + Time.currentTime() + ");");
    }

    @Override
    public boolean hasPlotOwnerOrMember(int plotId) {
        return plotSQL.hasRow("SELECT 1 FROM plot_members WHERE id=" + plotId + ";");
    }

    @Override
    public boolean isZonePublic(int zoneId) {
        return plotSQL.hasRow("SELECT 1 FROM zones WHERE id=" + zoneId + " AND is_public=1;");
    }

    @Override
    public int getPlotCoordinate(int plotId) {
        return plotSQL.getInt("SELECT coordinate_id FROM plot_data WHERE id=" + plotId + ";");
    }

    @Override
    public void updatePlotCoordinate(int plotId, int coordinateId) {
        plotSQL.update("UPDATE plot_data SET coordinate_id=" + coordinateId + " WHERE id=" + plotId + ";");
    }

    @Override
    public boolean isZoneOwner(String uuid) {
        return plotSQL.hasRow("SELECT 1 FROM zone_members WHERE uuid='" + uuid + "' AND is_owner=1;");
    }

    @Override
    public int getNumberOfZones() {
        return plotSQL.getInt("SELECT count(id) FROM zones;");
    }

    @Override
    public boolean plotExists(int plotId) {
        return plotSQL.hasRow("SELECT 1 FROM plot_data WHERE id=" + plotId + ";");
    }

    @Override
    public List<String> getBookPages(int bookId) {
        return plotSQL.getStringList("SELECT contents FROM book_data WHERE id=" + bookId + " ORDER BY page ASC;");
    }

    @Override
    public void updatePlotCategoryFeedback(int reviewId, String category, String selection, int bookId) {
        plotSQL.update("UPDATE plot_category_feedback SET category='" + category + "', selection='" + selection + "', book_id=" + bookId + " WHERE review_id=" + reviewId + ";");
    }

    @Override
    public int getHighestBookId() {
        return plotSQL.getInt("SELECT MAX(id) FROM book_data;");
    }

    @Override
    public int getLocationCoordMin(String location) {
        return plotSQL.getInt("SELECT coordMin FROM location_data WHERE name='" + location + "';");
    }

    @Override
    public int getLocationCoordMax(String location) {
        return plotSQL.getInt("SELECT coordMax FROM location_data WHERE name='" + location + "';");
    }

    @Override
    public List<String> getLocationRegions(String location) {
        return plotSQL.getStringList("SELECT region FROM regions WHERE location='" + location + "';");
    }

    @Override
    public boolean locationExists(String location) {
        return plotSQL.hasRow("SELECT name FROM location_data WHERE name='" + location + "';");
    }

    @Override
    public void deleteLocation(String location) {
        plotSQL.update("DELETE FROM location_data WHERE name='" + location + "';");
    }

    @Override
    public void deleteRegionsForLocation(String location) {
        plotSQL.update("DELETE FROM regions WHERE location='" + location + "';");
    }

    @Override
    public void updateLastSubmit(String uuid, long time) {
        globalSQL.update("UPDATE player_data SET last_submit=" + time + " WHERE uuid='" + uuid + "';");
    }

    @Override
    public long getLastSubmit(String uuid) {
        return globalSQL.getLong("SELECT last_submit FROM player_data WHERE uuid='" + uuid + "';");
    }

    @Override
    public void createPlotSubmission(int plotId) {
        plotSQL.createPlotSubmission(plotId);
    }

    @Override
    public int getDeniedPlotCount(int plotId, String uuid) {
        return plotSQL.getInt("SELECT COUNT(attempt) FROM plot_review WHERE plot_id=" + plotId + " AND uuid='" + uuid + "' AND completed=1 AND accepted=0;");
    }

    @Override
    public String getPlotReviewer(int plotId, String uuid, int attempt) {
        return plotSQL.getString("SELECT reviewer FROM plot_review WHERE plot_id=" + plotId + " AND uuid='" + uuid + "' AND attempt=" + attempt + ";");
    }

    @Override
    public String getPlotReviewer(int reviewId) {
        return plotSQL.getString("SELECT reviewer FROM plot_review WHERE review_id=" + reviewId + ";");
    }

    @Override
    public int getReviewId(int plotId, String uuid, int attempt) {
        return plotSQL.getInt("SELECT id FROM plot_review WHERE plot_id=" + plotId + " AND uuid='" + uuid + "' AND attempt=" + attempt + ";");
    }

    @Override
    public List<ReviewCategory> getReviewCategories(int reviewId) {
        List<ReviewCategory> reviewCategories = new ArrayList<>();

        List<String> categories = plotSQL.getStringList("SELECT category FROM plot_category_feedback WHERE review_id=" + reviewId + ";");

        for (String category : categories) {
            try {
                reviewCategories.add(ReviewCategory.valueOf(category));
            } catch (IllegalArgumentException e) {
                // Ignore, don't add the category to the list since it is no longer a valid category.
            }
        }

        return reviewCategories;
    }

    @Override
    public ReviewSelection getReviewSelection(int reviewId, ReviewCategory category) {
        String selection = plotSQL.getString("SELECT selection FROM plot_category_feedback WHERE review_id=" + reviewId + " AND category='" + category + "';");
        ReviewSelection reviewSelection;

        try {
            reviewSelection = ReviewSelection.valueOf(selection);
        } catch (IllegalArgumentException e) {
            reviewSelection = ReviewSelection.NONE;
        }

        return reviewSelection;
    }

    @Override
    public int getReviewBookId(int reviewId, ReviewCategory category) {
        return plotSQL.getInt("SELECT book_id FROM plot_category_feedback WHERE review_id=" + reviewId + " AND category='" + category + "';");
    }

    @Override
    public List<ReviewCategory> getVerificationCategories(int verificationId) {
        List<ReviewCategory> verificationCategories = new ArrayList<>();

        List<String> categories = plotSQL.getStringList("SELECT category FROM plot_verification_category WHERE verification_id=" + verificationId + ";");

        for (String category : categories) {
            try {
                verificationCategories.add(ReviewCategory.valueOf(category));
            } catch (IllegalArgumentException e) {
                // Ignore, don't add the category to the list since it is no longer a valid category.
            }
        }

        return verificationCategories;
    }

    @Override
    public ReviewSelection getVerificationSelectionOld(int verificationId, ReviewCategory category) {
        String selection = plotSQL.getString("SELECT selection_old FROM plot_verification_category WHERE verification_id=" + verificationId + " AND category='" + category + "';");
        ReviewSelection reviewSelection;

        try {
            reviewSelection = ReviewSelection.valueOf(selection);
        } catch (IllegalArgumentException e) {
            reviewSelection = ReviewSelection.NONE;
        }

        return reviewSelection;
    }

    @Override
    public int getVerificationBookIdOld(int verificationId, ReviewCategory category) {
        return plotSQL.getInt("SELECT book_id_old FROM plot_category_feedback WHERE verification_id=" + verificationId + " AND category='" + category + "';");
    }

    @Override
    public ReviewSelection getVerificationSelectionNew(int verificationId, ReviewCategory category) {
        String selection = plotSQL.getString("SELECT selection_new FROM plot_verification_category WHERE verification_id=" + verificationId + " AND category='" + category + "';");
        ReviewSelection reviewSelection;

        try {
            reviewSelection = ReviewSelection.valueOf(selection);
        } catch (IllegalArgumentException e) {
            reviewSelection = ReviewSelection.NONE;
        }

        return reviewSelection;
    }

    @Override
    public int getVerificationBookIdNew(int verificationId, ReviewCategory category) {
        return plotSQL.getInt("SELECT book_id_new FROM plot_category_feedback WHERE verification_id=" + verificationId + " AND category='" + category + "';");
    }
}
