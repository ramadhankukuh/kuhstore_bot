package com.kuhstore.bot;

import com.kuhstore.dao.BroadcastDAO;
import com.kuhstore.dao.MemberDAO;
import com.kuhstore.model.Broadcast;
import com.kuhstore.model.Member;
import java.util.List;

/**
 * Service untuk mengirim broadcast pesan ke semua member Telegram.
 */
public class BroadcastService {

    private final KuhStoreBot bot;
    private final MemberDAO memberDAO;
    private final BroadcastDAO broadcastDAO;

    public BroadcastService(KuhStoreBot bot) {
        this.bot = bot;
        this.memberDAO = new MemberDAO();
        this.broadcastDAO = new BroadcastDAO();
    }

    /**
     * Kirim broadcast ke semua member (atau member terverifikasi).
     *
     * @param title   Judul broadcast
     * @param content Isi pesan
     * @param sentBy  ID admin pengirim
     * @param onlyVerified true jika hanya ke member terverifikasi
     */
    public void sendBroadcast(String title, String content, int sentBy, boolean onlyVerified) {
        new Thread(() -> {
            try {
                List<Member> members;
                if (onlyVerified) {
                    members = memberDAO.findVerified();
                } else {
                    members = memberDAO.findAll();
                }

                int successCount = 0;
                for (Member member : members) {
                    try {
                        String message = "<b>📢 " + (title != null && !title.isEmpty() ? title : "Broadcast") + "</b>\n\n" + content;
                        bot.sendMessage(member.getTelegramId(), message);
                        successCount++;
                    } catch (Exception e) {
                        System.err.println("Failed to send to member " + member.getId() + ": " + e.getMessage());
                    }
                }

                // Simpan ke database
                Broadcast broadcast = new Broadcast();
                broadcast.setTitle(title);
                broadcast.setContent(content);
                broadcast.setSentBy(sentBy);
                broadcast.setRecipientCount(successCount);
                broadcastDAO.insert(broadcast);

                System.out.println("Broadcast sent to " + successCount + " recipients.");
            } catch (Exception e) {
                System.err.println("Error sending broadcast: " + e.getMessage());
            }
        }).start();
    }
}
