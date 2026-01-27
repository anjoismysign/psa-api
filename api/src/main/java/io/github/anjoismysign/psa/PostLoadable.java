package io.github.anjoismysign.psa;

public interface PostLoadable {
    /**
     * Runs in the same thread of the read operation.
     */
    void onPostLoad();
}