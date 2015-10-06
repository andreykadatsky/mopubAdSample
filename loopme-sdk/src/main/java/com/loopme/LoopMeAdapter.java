package com.loopme;

/**
 * Used when @link LoopMeBanner integrated in ListView/GridView
 * For correct work, ListView/GridView custom adaptor should implement
 * this interface.
 */
public interface LoopMeAdapter {

    /**
     * Detects is element of ListView/GridView Ad or not
     *
     * @param i - element position in list
     * @return - true - if element is Ad
     * false - otherwise
     */
    public boolean isAd(int i);
}
