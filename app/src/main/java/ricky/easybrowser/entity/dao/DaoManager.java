package ricky.easybrowser.entity.dao;

public class DaoManager {
    public static void createDefaultSiteList(AppDatabase db) {
        try {
            WebSite facebook = new WebSite(null, "Facebook", "https://www.facebook.com");
            WebSite google = new WebSite(null, "Google", "https://www.google.com");
            WebSite instagram = new WebSite(null, "Instagram", "https://www.instagram.com");
            WebSite wikipedia = new WebSite(null, "Wikipedia", "https://www.wikipedia.org");
            WebSite business = new WebSite(null, "Business", "https://www.business.com");
            WebSite twitter = new WebSite(null, "Twitter", "https://www.twitter.com");
            WebSite youtube = new WebSite(null, "YouTube", "https://www.youtube.com");
            WebSite gmail = new WebSite(null, "Gmail", "https://mail.google.com");
            db.webSiteDao().insertAllWebSite(facebook, google, instagram, wikipedia, business, twitter, youtube, gmail);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
