package net.bteuk.network.api.plotsystem;

/**
 * Utility class for finding the next role a user can achieve in the plot system based on their current role and the difficulty of the plot.
 */
public final class PromotionRoles {

    private PromotionRoles() {
    }

    /**
     * Returns the next role a user can achieve in the plot system based on their current role and the difficulty of the plot.
     *
     * @param plotDifficulty the difficulty of the plot
     * @param currentRole    the current role of the player
     * @return the next role the player can achieve, if any, else null
     */
    public static String getNewRole(int plotDifficulty, String currentRole) {
        String newRole = null;
        switch (plotDifficulty) {
            case 1 -> {
                if (currentRole.equals("applicant")) {
                    newRole = "apprentice";
                }
            }
            case 2 -> {
                if (currentRole.equals("applicant") || currentRole.equals("apprentice")) {
                    newRole = "jrbuilder";
                }
            }
            case 3 -> {
                if (currentRole.equals("applicant") || currentRole.equals("apprentice") || currentRole.equals("jrbuilder")) {
                    newRole = "builder";
                }
            }
        }
        return newRole;
    }
}
