package net.bteuk.network.sql;

import lombok.extern.java.Log;
import net.bteuk.network.api.plotsystem.SubmittedPlot;
import net.bteuk.network.api.plotsystem.SubmittedStatus;
import net.bteuk.network.core.Time;
import net.bteuk.network.core.sql.AbstractSQL;
import net.bteuk.network.lib.enums.PlotDifficulties;
import net.bteuk.network.lib.utils.Reviewing;
import net.bteuk.network.utils.TutorialRecommendation;
import teachingtutorials.utils.DBConnection;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static net.buildtheearth.terraminusminus.TerraMinusMinus.LOGGER;

@Log
public class PlotSQL extends AbstractSQL {

    public PlotSQL(DataSource datasource) {
        super(datasource);
    }

    public int[][] getPlotCorners(int plotID) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement("SELECT COUNT(corner) FROM" + " plot_corners WHERE id=" + plotID + ";");
                ResultSet results = statement.executeQuery()
        ) {

            results.next();

            int[][] corners = new int[results.getInt(1)][2];

            getPlotCorners(corners, plotID);

            return corners;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private int[][] getPlotCorners(int[][] corners, int plotID) {

        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement("SELECT x,z FROM " + "plot_corners WHERE id=" + plotID + ";");
                ResultSet results = statement.executeQuery()
        ) {

            for (int i = 0; i < corners.length; i++) {

                results.next();
                corners[i][0] = results.getInt(1);
                corners[i][1] = results.getInt(2);
            }

            return corners;
        } catch (SQLException e) {
            e.printStackTrace();
            return corners;
        }
    }

    // Creates a new plot and returns the id of the plot.
    public int createPlot(int size, int difficulty, String location, int coordinate_id) {

        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement("INSERT INTO plot_data" + "(status, size, difficulty, location, coordinate_id) VALUES(?, ?, ?, ?, ?);",
                        Statement.RETURN_GENERATED_KEYS)
        ) {

            statement.setString(1, "unclaimed");
            statement.setInt(2, size);
            statement.setInt(3, difficulty);
            statement.setString(4, location);
            statement.setInt(5, coordinate_id);
            statement.executeUpdate();

            // If the id does not exist return 0.
            try (ResultSet results = statement.getGeneratedKeys()) {
                if (results.next()) {

                    return results.getInt(1);
                } else {

                    return 0;
                }
            }
        } catch (SQLException sql) {

            sql.printStackTrace();
            return 0;
        }
    }

    // Creates a new plot and returns the id of the plot.
    public int createZone(String location, long expiration, boolean is_public) {

        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement("INSERT INTO zones" + "(location,expiration,is_public) VALUES(?, ?, ?);", Statement.RETURN_GENERATED_KEYS)
        ) {

            statement.setString(1, location);
            statement.setLong(2, expiration);
            statement.setBoolean(3, is_public);
            statement.executeUpdate();

            // If the id does not exist return 0.
            try (ResultSet results = statement.getGeneratedKeys()) {
                if (results.next()) {

                    return results.getInt(1);
                } else {

                    return 0;
                }
            }
        } catch (SQLException sql) {

            sql.printStackTrace();
            return 0;
        }
    }

    public double getReviewerReputation(String uuid) {
        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement("SELECT reputation FROM " + "reviewers WHERE uuid=?;")
        ) {
            statement.setString(1, uuid);
            ResultSet results = statement.executeQuery();

            if (results.next()) {
                return results.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Returns an unsorted list of submitted plots that are reviewable by the user of the uuid.
     *
     * @param uuid        the uuid of the user
     * @param isArchitect whether the user has Architects permissions
     * @param isReviewer  whether the user has Reviewer permissions
     * @return the list of submitted plot
     */
    public List<SubmittedPlot> getReviewablePlots(String uuid, boolean isArchitect, boolean isReviewer) {
        List<PlotDifficulties> difficulties = Reviewing.getAvailablePlotDifficulties(isArchitect, isReviewer, getReviewerReputation(uuid));

        List<SubmittedPlot> submittedPlots = new ArrayList<>();

        for (PlotDifficulties difficulty : difficulties) {
            submittedPlots.addAll(getSubmittedPlots(difficulty));
        }

        // Get all plots that the user is the owner or a member of, don't use those in the count.
        List<Integer> memberPlotIds = getIntList("SELECT id FROM plot_members WHERE uuid='" + uuid + "';");
        submittedPlots.removeIf(submittedPlot -> memberPlotIds.contains(submittedPlot.id()));

        return submittedPlots;
    }

    public List<SubmittedPlot> getSubmittedPlots(PlotDifficulties plotDifficulty) {
        ArrayList<SubmittedPlot> list = new ArrayList<>();
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement(
                        "SELECT pd.id,ps.submit_time FROM plot_data AS pd INNER JOIN plot_submission AS ps ON " + "pd.id=ps.plot_id WHERE ps.status='submitted' AND pd" +
                                ".difficulty='" + plotDifficulty.getValue() + "';");
                ResultSet results = statement.executeQuery()
        ) {
            while (results.next()) {
                list.add(new SubmittedPlot(results.getInt(1), results.getLong(2)));
            }
        } catch (SQLException e) {
            LOGGER.error("An error occurred while getting a list of submitted plots: ", e);
        }
        return list;
    }

    public List<Integer> getVerifiablePlots(String uuid, boolean isReviewer) {
        List<PlotDifficulties> difficulties = Reviewing.getAvailablePlotDifficulties(isReviewer, isReviewer, getReviewerReputation(uuid));

        List<Integer> plots_awaiting_verification = new ArrayList<>();

        for (PlotDifficulties difficulty : difficulties) {
            plots_awaiting_verification.addAll(getIntList(
                    "SELECT pd.id FROM plot_data AS pd INNER JOIN " + "plot_submission AS ps ON pd.id=ps.plot_id WHERE ps.status='awaiting verification' AND pd" + ".difficulty=" + difficulty.getValue() + " ORDER BY ps.submit_time ASC;"));
        }

        // Get all plots that the user is the owner or a member of, don't use those in the count.
        List<Integer> member_plots = getIntList("SELECT id FROM plot_members WHERE uuid='" + uuid + "';");

        // Get all plots that the user has reviewed, don't use those in the count.
        List<Integer> reviewed_plots = getIntList("SELECT plot_id FROM plot_review WHERE reviewer='" + uuid + "' AND " + "completed=0;");

        plots_awaiting_verification.removeAll(member_plots);
        plots_awaiting_verification.removeAll(reviewed_plots);

        return plots_awaiting_verification;
    }

    public int getReviewablePlotCount(String uuid, boolean isArchitect, boolean isReviewer) {
        return getReviewablePlots(uuid, isArchitect, isReviewer).size();
    }

    public int getVerifiablePlotCount(String uuid, boolean isReviewer) {
        return getVerifiablePlots(uuid, isReviewer).size();
    }

    public void addOrUpdateReviewer(String uuid, String roleId) {
        // Check if the reviewer is already added to the table.
        boolean hasRow = hasRow("SELECT 1 FROM reviewers WHERE uuid='" + uuid + "';");

        double initialValue = getReviewerReputation(uuid);

        // Reviewer reputation must always start at 5. But don't decrease if already above 5.
        if (roleId.equals("reviewer") && initialValue < 5) {
            initialValue = 5;
        }

        // If an entry already exists, update it, if promoted to reviewer.
        // If no entry exists, add a new entry.
        // Else do nothing.
        if (hasRow && roleId.equals("reviewer")) {
            update("UPDATE reviewers SET reputation=" + initialValue + " WHERE uuid='" + uuid + "';");
        } else if (!hasRow) {
            update("INSERT INTO reviewers(uuid,reputation) VALUES('" + uuid + "'," + initialValue + ");");
        }
    }

    /**
     * Determines whether a specific player can review a specific plot.
     *
     * @param plotId      the plot ID to check
     * @param uuid        the player uuid to check
     * @param isArchitect if the player has architect permissions
     * @param isReviewer  if the player has reviewer permissions
     * @return whether the player can review this plot
     */
    public boolean canReviewPlot(int plotId, String uuid, boolean isArchitect, boolean isReviewer) {
        List<PlotDifficulties> difficulties = Reviewing.getAvailablePlotDifficulties(isArchitect, isReviewer, getReviewerReputation(uuid));
        int plotDifficulty = getInt("SELECT difficulty FROM plot_data WHERE id=" + plotId + ";");

        // Check if the user can review a plot with this difficulty.
        // Check if the user is a member of the plot.
        return (difficulties.stream().mapToInt(PlotDifficulties::getValue).anyMatch(difficulty -> difficulty == plotDifficulty) && !hasRow(
                "SELECT id FROM plot_members WHERE id=" + plotId + " AND uuid='" + uuid + "';"));
    }

    public boolean canVerifyPlot(int plotId, String uuid, boolean isReviewer) {
        List<PlotDifficulties> difficulties = Reviewing.getAvailablePlotDifficulties(isReviewer, isReviewer, getReviewerReputation(uuid));
        int plotDifficulty = getInt("SELECT difficulty FROM plot_data WHERE id=" + plotId + ";");

        // The player must be a reviewer, is not the reviewer of the plot and is not the owner or a member of the plot.
        // The reviewer must also be allowed to verify a plot of this difficulty.
        return (isReviewer && difficulties.stream().mapToInt(PlotDifficulties::getValue).anyMatch(difficulty -> difficulty == plotDifficulty) && !hasRow(
                "SELECT 1 FROM plot_review WHERE reviewer='" + uuid + "' AND plot_id=" + plotId + " AND " + "completed=0") && !hasRow(
                "SELECT 1 FROM plot_members WHERE id=" + plotId + " AND uuid='" + uuid + "';"));
    }

    /**
     * Creates a new plot review and the return the generated ID.
     *
     * @param plotId    the plot id
     * @param plotOwner the plot owner
     * @param reviewer  the reviewer
     * @param accepted  true if the plot should be accepted, false if denied
     * @param completed trie if the review is complete, false if a verification is required
     * @return the review id
     */
    public int createReview(int plotId, String plotOwner, String reviewer, boolean accepted, boolean completed) {
        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
                "INSERT INTO plot_review(plot_id,uuid,reviewer," + "attempt,review_time,accepted,completed) VALUES(?, ?, ?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS)
        ) {

            statement.setInt(1, plotId);
            statement.setString(2, plotOwner);
            statement.setString(3, reviewer);
            statement.setInt(4, (1 + getLatestAttempt(plotId, plotOwner)));
            statement.setLong(5, System.currentTimeMillis());
            statement.setBoolean(6, accepted);
            statement.setBoolean(7, completed);
            statement.executeUpdate();

            // If the id does not exist return 0.
            try (ResultSet results = statement.getGeneratedKeys()) {
                if (results.next()) {
                    return results.getInt(1);
                } else {
                    return 0;
                }
            }
        } catch (SQLException sql) {
            sql.printStackTrace();
            return 0;
        }
    }

    private int getLatestAttempt(int plotId, String plotOwner) {
        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement("SELECT COUNT(1) FROM plot_review WHERE plot_id=?" + " AND uuid=?;")
        ) {
            statement.setInt(1, plotId);
            statement.setString(2, plotOwner);

            ResultSet results = statement.executeQuery();
            if (results.next()) {
                return results.getInt(1);
            }
        } catch (SQLException e) {
            // Assume that no previous attempts have been made.
        }
        return 0;
    }

    public boolean savePlotReviewCategoryFeedback(int reviewId, String category, String selection, int bookId) {
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement("INSERT INTO plot_category_feedback(review_id," + "category,selection,book_id) VALUES(?, ?, ?, ?);")
        ) {

            statement.setInt(1, reviewId);
            statement.setString(2, category);
            statement.setString(3, selection);
            statement.setInt(4, bookId);
            statement.executeUpdate();
        } catch (SQLException sql) {
            sql.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Creates a new plot verification and the return the generated ID.
     *
     * @param reviewId    the review id
     * @param verifier    the review verifier
     * @param acceptedOld true if the reviewer accepted the plot
     * @param acceptedNew true if the verifier accepted the plot
     * @return the verification id
     */
    public int createVerification(int reviewId, String verifier, boolean acceptedOld, boolean acceptedNew) {
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement("INSERT INTO plot_verification(review_id," + "verifier,accepted_old,accepted_new) VALUES(?, ?, ?, ?);",
                        Statement.RETURN_GENERATED_KEYS)
        ) {
            statement.setInt(1, reviewId);
            statement.setString(2, verifier);
            statement.setBoolean(3, acceptedOld);
            statement.setBoolean(4, acceptedNew);
            statement.executeUpdate();

            // If the id does not exist return 0.
            try (ResultSet results = statement.getGeneratedKeys()) {
                if (results.next()) {
                    return results.getInt(1);
                } else {
                    return 0;
                }
            }
        } catch (SQLException sql) {
            sql.printStackTrace();
            return 0;
        }
    }

    /**
     * Save a plot verification category, if the verifier edited the feedback in any way.
     *
     * @param verificationId the id of the verification
     * @param category       the category that was altered
     * @param selectionOld   the selection the reviewer made
     * @param selectionNew   the selection the verifier made
     * @param bookOld        the book id of the reviewers feedback
     * @param bookNew        the book id of the verifiers feedback
     */
    public boolean savePlotVerificationCategory(int verificationId, String category, String selectionOld, String selectionNew, int bookOld, int bookNew) {
        try (
                Connection conn = conn(); PreparedStatement statement = conn.prepareStatement(
                "INSERT INTO plot_verification_category" + "(verification_id,category,selection_old,selection_new,book_id_old,book_id_new) VALUES(?, ?, ?, ?, ?, ?);")
        ) {
            statement.setInt(1, verificationId);
            statement.setString(2, category);
            statement.setString(3, selectionOld);
            statement.setString(4, selectionNew);
            statement.setInt(5, bookOld);
            statement.setInt(6, bookNew);
            statement.executeUpdate();
        } catch (SQLException sql) {
            sql.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean saveBook(int id, int page, String contents) {
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement("INSERT INTO book_data(id,page,contents) VALUES(?, ?, ?);")
        ) {
            statement.setInt(1, id);
            statement.setInt(2, page);
            statement.setString(3, contents);
            statement.executeUpdate();
        } catch (SQLException sql) {
            LOGGER.error("An error occurred when executing insert statement: ", sql);
            return false;
        }
        return true;
    }

    /**
     * Fetches a list of tutorial recommendations for a given plot
     *
     * @param tutorialsDBConnection Database connection for tutorials
     * @param iPlotID               The ID of the plot to fetch the recommended tutorials of
     * @return A list of tutorial recommendations
     */
    public TutorialRecommendation[] fetchTutorialRecommendationsForPlot(DBConnection tutorialsDBConnection, int iPlotID) {
        // SQL objects
        String sql;
        ResultSet resultSet;

        TutorialRecommendation[] recommendations;

        // A count of the number of tutorial recommendations for this plot
        int iCount;

        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement("SELECT * FROM tutorial_recommendations WHERE plot_id = " + iPlotID)
        ) {
            sql = "SELECT Count(1) FROM tutorial_recommendations WHERE plot_id = " + iPlotID;
            iCount = getInt(sql);
            recommendations = new TutorialRecommendation[iCount];

            resultSet = statement.executeQuery();
            for (int i = 0; i < iCount; i++) {
                resultSet.next();
                recommendations[i] = new TutorialRecommendation(tutorialsDBConnection, resultSet.getInt("recommendation_id"), iPlotID);
            }
        } catch (SQLException e) {
            log.warning("Error fetching tutorial recommendations for plot " + iPlotID + ": " + e.getLocalizedMessage());
            return new TutorialRecommendation[0];
        }

        return recommendations;
    }

    public boolean createLocation(String locationName, String alias, String server, int coordMin, int coordMax, int xTransform, int yTransform) {
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement(
                        "INSERT INTO location_data" + "(name, alias, server, coordMin, coordMax, xTransform, zTransform) VALUES(?, ?, ?, ?, ?, ?, ?);")
        ) {

            statement.setString(1, locationName);
            statement.setString(2, alias);
            statement.setString(3, server);
            statement.setInt(4, coordMin);
            statement.setInt(5, coordMax);
            statement.setInt(6, xTransform);
            statement.setInt(7, yTransform);
            statement.executeUpdate();
            return true;

        } catch (SQLException sql) {
            log.severe("An error occurred while creating a location: " + sql);
            return false;
        }
    }

    public boolean createRegion(String regionName, String server, String locationName) {
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement("INSERT INTO regions" + "(region, server, location) VALUES(?, ?, ?);")
        ) {

            statement.setString(1, regionName);
            statement.setString(2, server);
            statement.setString(3, locationName);
            statement.executeUpdate();
            return true;

        } catch (SQLException sql) {
            log.severe("An error occurred while creating a region: " + sql);
            return false;
        }
    }

    public boolean updateLocationAlias(String location, String alias) {
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement("UPDATE location_data SET alias=? WHERE name=?;")
        ) {

            statement.setString(1, alias);
            statement.setString(2, location);
            statement.executeUpdate();
            return true;

        } catch (SQLException sql) {
            log.severe("An error occurred while updating a location alias: " + sql);
            return false;
        }
    }

    public boolean createPlotSubmission(int plotId) {
        try (
                Connection conn = conn();
                PreparedStatement statement = conn.prepareStatement("INSERT INTO plot_submission(plot_id,submit_time,status,last_query) VALUES(?, ?, ?, ?);")
        ) {

            statement.setInt(1, plotId);
            statement.setLong(2, Time.currentTime());
            statement.setString(3, SubmittedStatus.SUBMITTED.getDatabaseValue());
            statement.setLong(4, Time.currentTime());
            statement.executeUpdate();
            return true;

        } catch (SQLException sql) {
            log.severe("An error occurred while creating a location: " + sql);
            return false;
        }
    }
}
