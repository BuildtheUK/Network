package net.bteuk.network.api.impl;

import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.sql.PlotSQL;

import java.util.List;

public class PlotAPIImpl implements PlotAPI {

    private final PlotSQL plotSQL;

    public PlotAPIImpl(PlotSQL plotSQL) {
        this.plotSQL = plotSQL;
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
    public boolean createLocation(String locationName, String alias, String server, int coordMin, int coordMax, int xTransform, int yTransform) {

        return false;
    }

    @Override
    public boolean createPlotRegion(String regionName, String server, String locationName) {
        return false;
    }

    @Override
    public boolean setLocationAlias(String locationName, String alias) {
        return false;
    }

    @Override
    public boolean setPlotDifficulty(int plotId, int difficulty) {
        return false;
    }

    @Override
    public boolean clearZoneMembers(int zoneId) {
        return false;
    }

    @Override
    public boolean setPlotStatus(int plotId, String status) {
        return false;
    }

    @Override
    public boolean setZoneStatus(int zoneId, String status) {
        return false;
    }

    @Override
    public boolean clearPlotMembers(int plotId) {
        return false;
    }

    @Override
    public boolean setPlotSubmissionStatus(int plotId, String status) {
        return false;
    }

    @Override
    public boolean removePlotSubmission(int plotId) {
        return false;
    }

    @Override
    public boolean createPlotMember(int plotId, String uuid) {
        return false;
    }

    @Override
    public boolean removePlotMember(int plotId, String uuid) {
        return false;
    }

    @Override
    public boolean createZoneOwner(int zoneId, String uuid) {
        return false;
    }

    @Override
    public boolean createZoneMember(int zoneId, String uuid) {
        return false;
    }

    @Override
    public boolean removeZoneMember(int zoneId, String uuid) {
        return false;
    }

    @Override
    public boolean setPlotLastEnter(int plotId, String uuid) {
        return false;
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
        return false;
    }

    @Override
    public boolean createPlotCorner(int plotId, int cornerIndex, int x, int z) {
        return false;
    }

    @Override
    public boolean createZoneCorner(int zoneId, int cornerIndex, int x, int z) {
        return false;
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
    public String getPlotOwner(int plotID) {
        return plotSQL.getString("SELECT uuid FROM plot_members WHERE id=" + plotID + " AND is_owner=1;");
    }

    @Override
    public String getZoneOwner(int zoneID) {
        return plotSQL.getString("SELECT uuid FROM zone_members WHERE id=" + zoneID + " AND is_owner=1;");
    }

    @Override
    public boolean isPlotOwner(int plotID, String uuid) {
        return plotSQL.hasRow("SELECT id FROM plot_members WHERE id=" + plotID + " AND uuid='" + uuid + "' AND is_owner=1;");
    }

    @Override
    public boolean isPlotMember(int plotID, String uuid) {
        return plotSQL.hasRow("SELECT id FROM plot_members WHERE id=" + plotID + " AND uuid='" + uuid + "' AND is_owner=0;");

    }

    @Override
    public boolean isPlotClaimed(int plotID) {
        return plotSQL.hasRow("SELECT id FROM plot_data WHERE id=" + plotID + " AND status='claimed';");
    }

    @Override
    public int getNumberOfPlots(String uuid) {
        return plotSQL.getInt("SELECT count(id) FROM plot_members WHERE uuid='" + uuid + "';");
    }

}
