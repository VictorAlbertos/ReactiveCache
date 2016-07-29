package io.reactivecache;

import io.victoralbertos.jolyglot.JolyglotGenerics;
import io.victoralbertos.jolyglot.MoshiSpeaker;

public final class Jolyglot$ {
    public static JolyglotGenerics newInstance() {
        return new MoshiSpeaker();
    }
}
