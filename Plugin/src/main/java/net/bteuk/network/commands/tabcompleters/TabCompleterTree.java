package net.bteuk.network.commands.tabcompleters;

import java.util.ArrayList;
import java.util.List;

public class TabCompleterTree {
    private List<TabCompleterNode> rootOptions;

    /**
     * Contructor
     *
     * @param treeAsString the options that are available on typing written as a tree in the format "1starg1, 2ndarg1 (1starg2, 2ndarg2),3rdarg1"
     */
    public TabCompleterTree(String treeAsString) {

        rootOptions = parseString(treeAsString);

    }

    public List<String> getNextPossibleStrings(String[] args) {
        if (args.length == 0) {
            List<String> returnList = new ArrayList<>();
            for (TabCompleterNode n : rootOptions) {
                returnList.add(n.argument);
            }
            return returnList;
        }
        return getPossibleStringsHelper(args, 0, rootOptions);
    }

    private List<String> getPossibleStringsHelper(String[] args, int index, List<TabCompleterNode> nodes) {
        List<String> results = new ArrayList<>();
        for (TabCompleterNode n : nodes) {
            if (index == (args.length - 1)) {
                if (n.argument.startsWith(args[index])) {
                    results.add(n.argument);
                }
            } else {
                if (n.argument.equals(args[index])) {
                    return getPossibleStringsHelper(args, index + 1, n.children);
                }
            }
        }

        return results;
    }

    private List<TabCompleterNode> parseString(String input) {
        input = input.replaceAll("\\s", "");
        char[] inputWorking = input.toCharArray();
        StringBuilder current = new StringBuilder();
        int bracketsCount = 0;
        List<String> tokens = new ArrayList<String>();
        for (char currentChar : inputWorking) {
            if (currentChar == ',' && bracketsCount == 0) {
                tokens.add(current.toString());
                current = new StringBuilder();
            } else if (currentChar == '(') {
                current.append(' ');
                bracketsCount++;
            } else if (currentChar == ')') {
                bracketsCount--;
            } else {
                current.append(currentChar);
            }
        }
        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }
        List<TabCompleterNode> finalList = new ArrayList<TabCompleterNode>();
        for (String s : tokens) {
            if (s.contains(",")) {
                String[] parts = s.split(" ");
                finalList.add(new TabCompleterNode(parts[0], parseString(parts[1])));
            } else {
                finalList.add(new TabCompleterNode(s, new ArrayList<TabCompleterNode>()));
            }
        }
        return finalList;

    }
}
