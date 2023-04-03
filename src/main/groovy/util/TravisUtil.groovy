package util

class TravisUtil {
    static enum TravisStrategy{
        TRAVIS_SHALLOW_CLONE,
        TRAVIS_RETRY,
        TRAVIS_WAIT,
        TRAVIS_CACHE,
        TRAVIS_FAST_FINISH
    }
}
