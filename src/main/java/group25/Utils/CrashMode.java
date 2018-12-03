package group25.Utils;

public enum CrashMode {
    NO_CRASH, // 0

    TM_BEFORE_VOTE_REQUEST, // 1
    TM_BEFORE_ANY_VOTE_REPLIES, // 2
    TM_BEFORE_SOME_VOTE_REPLIES, // 3
    TM_BEFORE_DECIDING, // 4
    TM_BEFORE_SENDING_DECISION, // 5
    TM_BEFORE_SENDING_LAST_DECISION, // 6
    TM_AFTER_SENDING_ALL_DECISIONS, // 7
    TM_DURING_RECOVERY, // 8

    RM_BEFORE_DECIDING_VOTE, // 9
    RM_AFTER_DECIDING_VOTE, // 10
    RM_AFTER_VOTING, // 11
    RM_AFTER_RECEIVING_DECISION, // 12
    RM_DURING_RECOVERY, // 13
}