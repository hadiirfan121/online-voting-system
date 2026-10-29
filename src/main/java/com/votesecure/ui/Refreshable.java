package com.votesecure.ui;

/** Screens implement this to reload live data each time they are shown. */
public interface Refreshable {

    void onShow();
}
