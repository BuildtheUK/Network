package net.bteuk.network.gui;

import net.bteuk.network.utils.NetworkUser;

@FunctionalInterface
public interface NetworkGuiAction {
    void click(NetworkUser networkUser);
}
