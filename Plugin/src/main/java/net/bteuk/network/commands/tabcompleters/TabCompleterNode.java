package net.bteuk.network.commands.tabcompleters;

import java.util.List;

public class TabCompleterNode {

    public String argument;
    public List<TabCompleterNode> children;
    public TabCompleterNode(String d, List<TabCompleterNode> ch)
    {
        argument = d;
        children = ch;
    }


}
