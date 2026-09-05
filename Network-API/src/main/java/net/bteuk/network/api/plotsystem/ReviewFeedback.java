package net.bteuk.network.api.plotsystem;

import net.bteuk.network.api.PlotAPI;
import net.bteuk.network.api.SQLAPI;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.btuk.network.lib.utils.ChatUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to construct a feedback book based on a plot review.
 */
public final class ReviewFeedback {

    private static final Component REVIEW_BOOK_TITLE = ChatUtils.title("Review Book");

    private ReviewFeedback() {
        // Private constructor.
    }

    /**
     * Create the feedback book for a plot review.
     *
     * @param globalSQL access to the global database
     * @param plotAPI   plot API
     * @param reviewId  the id of the plot review
     * @return the feedback book for the plot review
     */
    public static Book createFeedbackBook(SQLAPI globalSQL, PlotAPI plotAPI, int reviewId) {

        Component firstPage = Component.empty();

        // Title
        firstPage =
                firstPage.append(
                        Component.text("Feedback").decorate(TextDecoration.UNDERLINED).decorate(TextDecoration.BOLD)
                                .appendNewline().appendNewline());

        // Reviewer
        String reviewer = globalSQL.getString("SELECT name FROM player_data WHERE uuid='" + plotAPI.getPlotReviewer(reviewId) + "';");
        firstPage = firstPage.append(Component.text(String.format("Reviewer: %s", reviewer),
                NamedTextColor.DARK_GRAY)).appendNewline();

        List<Component> pages = new ArrayList<>();

        // Add each category that has a selection.
        Map<ReviewCategory, ReviewCategoryFeedback> reviewCategoryFeedback = getReviewCategoryFeedback(plotAPI, reviewId);
        for (ReviewCategory category : ReviewCategory.values()) {
            ReviewCategoryFeedback categoryFeedback = reviewCategoryFeedback.get(category);
            if (categoryFeedback == null) {
                continue;
            }
            firstPage = firstPage.appendNewline();
            // Add the category to the book.
            firstPage = firstPage.append(addCategoryToFeedbackBook(plotAPI, categoryFeedback, pages));
        }

        // Insert the first page of the book at the start.
        pages.addFirst(firstPage);

        return Book.book(REVIEW_BOOK_TITLE, ChatUtils.line(reviewer), pages);
    }

    /**
     * Create the feedback book for a plot verification.
     *
     * @param verificationId the id of the plot verification
     * @param old            true if the before view should be created
     * @return the feedback book for the plot verification
     */
    public static Book createVerificationFeedbackBook(PlotAPI plotAPI, int verificationId, boolean old) {

        Component firstPage = Component.empty();

        // Title
        firstPage =
                firstPage.append(
                        Component.text("Feedback").decorate(TextDecoration.UNDERLINED).decorate(TextDecoration.BOLD)
                                .appendNewline().appendNewline());

        List<Component> pages = new ArrayList<>();

        // Add each category that has a selection.
        Map<ReviewCategory, ReviewCategoryFeedback> verificationCategoryFeedback =
                getVerificationCategoryFeedback(plotAPI, verificationId, old);
        for (ReviewCategory category : ReviewCategory.values()) {
            ReviewCategoryFeedback categoryFeedback = verificationCategoryFeedback.get(category);
            if (categoryFeedback == null) {
                continue;
            }
            firstPage = firstPage.appendNewline();
            // Add the category to the book.
            firstPage = firstPage.append(addCategoryToFeedbackBook(plotAPI, categoryFeedback, pages));
        }

        // Insert the first page of the book at the start.
        pages.addFirst(firstPage);

        return Book.book(REVIEW_BOOK_TITLE, Component.empty(), pages);
    }

    private static Map<ReviewCategory, ReviewCategoryFeedback> getReviewCategoryFeedback(PlotAPI plotAPI, int reviewId) {

        Map<ReviewCategory, ReviewCategoryFeedback> reviewCategoryFeedbackMap = new HashMap<>();

        // Get the feedback for the review.
        List<ReviewCategory> reviewCategories = plotAPI.getReviewCategories(reviewId);
        for (ReviewCategory category : reviewCategories) {
            reviewCategoryFeedbackMap.put(category,
                    new ReviewCategoryFeedback(category, plotAPI.getReviewSelection(reviewId, category), plotAPI.getReviewBookId(reviewId, category)));
        }

        return reviewCategoryFeedbackMap;
    }

    private static Map<ReviewCategory, ReviewCategoryFeedback> getVerificationCategoryFeedback(PlotAPI plotAPI, int verificationId,
                                                                                               boolean old) {

        Map<ReviewCategory, ReviewCategoryFeedback> reviewCategoryFeedbackMap = new HashMap<>();

        // Get the feedback for the review.
        List<ReviewCategory> verificationCategories = plotAPI.getVerificationCategories(verificationId);
        for (ReviewCategory category : verificationCategories) {
            if (old) {
                reviewCategoryFeedbackMap.put(category,
                        new ReviewCategoryFeedback(category, plotAPI.getVerificationSelectionOld(verificationId, category),
                                plotAPI.getVerificationBookIdOld(verificationId, category)));
            } else {
                reviewCategoryFeedbackMap.put(category, new ReviewCategoryFeedback(category, plotAPI.getVerificationSelectionNew(verificationId, category),
                        plotAPI.getVerificationBookIdNew(verificationId, category)));
            }
        }

        return reviewCategoryFeedbackMap;
    }

    @NotNull
    private static Component addCategoryToFeedbackBook(PlotAPI plotAPI, ReviewCategoryFeedback categoryFeedback, List<Component> pages) {

        Component line = Component.empty();
        Component category = Component.text(categoryFeedback.category().getDisplayName(),
                Style.style(TextDecoration.BOLD));

        // Add the feedback to the book if it exists.
        if (categoryFeedback.bookId() != 0) {
            List<String> sPages = plotAPI.getBookPages(categoryFeedback.bookId());

            category = category.clickEvent(getGotoFeedbackClickEvent(pages.size() + 2))
                    .hoverEvent(HoverEvent.showText(Component.text(String.format("Click to go view %s feedback.",
                            categoryFeedback.category().getDisplayName()))));
            category = category.color(NamedTextColor.DARK_BLUE).decorate(TextDecoration.UNDERLINED); // Blue colour
            // underlined to indicate that you can navigate to the feedback.
            pages.addAll(sPages.stream().map(Component::text).toList());

            if (categoryFeedback.selection() != ReviewSelection.NONE) {
                category = category.append(Component.text(":"));
            }
        }
        line = line.append(category);

        // Selection
        line = line.appendSpace();
        line = line.append(categoryFeedback.selection().getDisplayComponent());

        return line;
    }

    private static ClickEvent getGotoFeedbackClickEvent(int page) {
        return ClickEvent.changePage(page);
    }
}
