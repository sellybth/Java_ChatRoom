
import java.util.*;

public class SampleData {

    public static class Message {
        public final String sender;
        public final String content;
        public final String time;
        public final boolean isMine;

        public Message(String sender, String content, String time, boolean isMine) {
            this.sender  = sender;
            this.content = content;
            this.time    = time;
            this.isMine  = isMine;
        }
    }

    public static class Contact {
        public final String name;
        public final String avatar;   // initials
        public final String lastMsg;
        public final String time;
        public final int    unread;
        public final boolean online;
        public final boolean isGroup;

        public Contact(String name, String avatar, String lastMsg,
                       String time, int unread, boolean online, boolean isGroup) {
            this.name    = name;
            this.avatar  = avatar;
            this.lastMsg = lastMsg;
            this.time    = time;
            this.unread  = unread;
            this.online  = online;
            this.isGroup = isGroup;
        }
    }

    public static List<Contact> getContacts() {
        List<Contact> list = new ArrayList<>();
        list.add(new Contact("Arjun Mehta",   "AM", "haha yeah that was wild 😂",    "2:41 PM", 3,  true,  false));
        list.add(new Contact("Dev Team 🚀",    "DT", "Rahul: PR is up for review",   "2:30 PM", 1,  false, true));
        list.add(new Contact("Priya Sharma",  "PS", "okay sounds good, ttyl!",       "1:15 PM", 0,  true,  false));
        list.add(new Contact("Weekend Plans", "WP", "You: I'm in 🙌",                "12:08 PM",0,  false, true));
        return list;
    }

    public static Map<String, List<Message>> getConversations() {
        Map<String, List<Message>> map = new LinkedHashMap<>();

        // --- Arjun Mehta ---
        List<Message> arjun = new ArrayList<>();
        arjun.add(new Message("Arjun Mehta", "bro did you see the match last night??", "2:15 PM", false));
        arjun.add(new Message("Me",          "YES omg that last over was insane", "2:16 PM", true));
        arjun.add(new Message("Arjun Mehta", "I literally screamed when they hit that six 😭", "2:17 PM", false));
        arjun.add(new Message("Me",          "same lmao my neighbors probably hate me now", "2:18 PM", true));
        arjun.add(new Message("Arjun Mehta", "haha yeah that was wild 😂", "2:41 PM", false));
        map.put("Arjun Mehta", arjun);

        // --- Dev Team ---
        List<Message> dev = new ArrayList<>();
        dev.add(new Message("Rahul",  "hey team, deploying the new build at 3pm", "1:00 PM", false));
        dev.add(new Message("Me",     "sounds good, I'll keep an eye on the logs", "1:02 PM", true));
        dev.add(new Message("Nisha",  "same, and I'll run smoke tests right after", "1:05 PM", false));
        dev.add(new Message("Rahul",  "great 🙌 also reminder — standup moved to 10:30 tomorrow", "1:30 PM", false));
        dev.add(new Message("Me",     "noted 👍", "1:31 PM", true));
        dev.add(new Message("Rahul",  "PR is up for review", "2:30 PM", false));
        map.put("Dev Team 🚀", dev);

        // --- Priya Sharma ---
        List<Message> priya = new ArrayList<>();
        priya.add(new Message("Priya Sharma", "hey! are you coming to Zara's birthday thing?", "12:45 PM", false));
        priya.add(new Message("Me",           "yes!! already got her gift sorted 🎁", "12:47 PM", true));
        priya.add(new Message("Priya Sharma", "ooh what did you get?", "12:48 PM", false));
        priya.add(new Message("Me",           "those noise cancelling headphones she was eyeing", "12:49 PM", true));
        priya.add(new Message("Priya Sharma", "okay sounds good, ttyl!", "1:15 PM", false));
        map.put("Priya Sharma", priya);

        // --- Weekend Plans ---
        List<Message> wknd = new ArrayList<>();
        wknd.add(new Message("Arjun",  "okay so hiking or beach this weekend?", "11:00 AM", false));
        wknd.add(new Message("Kabir",  "beach 100%, it's gonna be 34 degrees", "11:05 AM", false));
        wknd.add(new Message("Sneha",  "hiking!! beach is too crowded on weekends", "11:10 AM", false));
        wknd.add(new Message("Me",     "I'm in 🙌", "12:08 PM", true));
        map.put("Weekend Plans", wknd);

        return map;
    }
}
