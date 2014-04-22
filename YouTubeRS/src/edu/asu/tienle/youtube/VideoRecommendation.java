package edu.asu.tienle.youtube;

import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.media.mediarss.MediaPlayer;
import com.google.gdata.data.youtube.FriendEntry;
import com.google.gdata.data.youtube.FriendFeed;
import com.google.gdata.data.youtube.UserEventEntry;
import com.google.gdata.data.youtube.UserEventFeed;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.YouTubeMediaGroup;

/*This class implements a YouTube recommendation system.
 * Input: an youtube username, userA.
 * 1) It creates a bipartite graph G=(U,V,E) where:
	 * U is the set of friends of the userA, 
	 * V is the set of all videos that users in U have interacted with. 
	 * E is the set of edges (userU,v) where userU is in U and v is in V and userU has interacted with v.   
 * 
 * 2) It uses HITS algorithm to calculate authority score for all vertices in G
 * 3) It selects top nVideo videos in V which have highest authority score to recommend to userA.
 * @author Tien D. Le
 */

public class VideoRecommendation {
   String username;
   int nVideo;//show top nVideo videos
   String []recommendedList;//recommended videos
   WebGraph g;
   YouTubeService service;
  
  public VideoRecommendation(String username){
	  this.username=username;
	  int nVideo=10;//show top ten videos
	  recommendedList=new String[nVideo];//recommended videos
	  g=new WebGraph();
  }
  
  public void recommend(){
	    /**
	    * The name of the server hosting the YouTube GDATA feeds.
	    */
	    final String YOUTUBE_GDATA_SERVER = "http://gdata.youtube.com";	  
	    /**
	    * The prefix of recent activity feeds
	    */
	    final String ACTIVITY_FEED_PREFIX = YOUTUBE_GDATA_SERVER+ "/feeds/api/events";


		//List<String>friendList=new ArrayList<String>();
		List<String>friendList=new LinkedList<String>();

		
		String feedUrl=null;
		try{
	        System.out.println(username);
			String clientID="Tienttt10";
			String developerKey="AI39si5pTs3Xed-K9kT7B8h74hIcSU5Vo64n_Qz-_S8grEMJXbYoN7uqPRlJUQaMkA280_kU3rNmyVB7KO7m9LA9crj4Mqs_rA";  
			service = new YouTubeService(clientID,developerKey);
			feedUrl ="https://gdata.youtube.com/feeds/api/users/"+username+"/contacts";
			
		  	FriendFeed friendFeed = service.getFeed(new URL(feedUrl), FriendFeed.class);
	        
		  	//update friendlist
		  	for(FriendEntry friendEntry : friendFeed.getEntries()) {
		  	friendList.add(friendEntry.getUsername());
		  	}
		  	
		  	for(String friend:friendList){	
		  		feedUrl= ACTIVITY_FEED_PREFIX + "?author=" + friend;
		  		updateGraph(feedUrl);
		  	}
	    	populateRecommendedList();//calculate HITS and update recommendedList;
	    }
		catch(Exception e){
            System.out.println(e.getMessage());
		}
	}

private void updateGraph(String feedUrl)throws Exception{//this method updates the graph g, for example, if userU uploaded a video v, then it adds an edge (userU,v) to the graph.  
	  UserEventFeed activityFeed = service.getFeed(new URL(feedUrl),UserEventFeed.class);
	  double upload_weight=1.0;
	  double rate_weight=1.0;
	  double fav_weight=1.0;
	  double share_weight=1.0;
	  double comment_weight=1.0;

	for (UserEventEntry entry : activityFeed.getEntries()) {
		  String user = entry.getAuthors().get(0).getName();
	      
		  if(entry.getUserEventType() == UserEventEntry.Type.VIDEO_UPLOADED) {
	    	g.addLink(user, entry.getVideoId(), upload_weight);
	      }
		  
	      else if(entry.getUserEventType() == UserEventEntry.Type.VIDEO_RATED) {
	        g.addLink(user, entry.getVideoId(), rate_weight);
	      }
	      
	      else if(entry.getUserEventType() == UserEventEntry.Type.VIDEO_FAVORITED) {
	        g.addLink(user, entry.getVideoId(), fav_weight);
	      }
	      else if(entry.getUserEventType() == UserEventEntry.Type.VIDEO_SHARED) {
	        g.addLink(user, entry.getVideoId(), share_weight);
	      }
		  
	      else if(entry.getUserEventType() == UserEventEntry.Type.VIDEO_COMMENTED) {
	        g.addLink(user, entry.getVideoId(), comment_weight);
	      }
	    }
}

private void populateRecommendedList(){//this method calls HITS algorithm, and select top nVideo with highest authorityScore
	HITS hits=new HITS(g);
	int nNode=g.numNodes();

	if(nNode>0){ 
		double []authority=new double[nNode];
		
		HashMap<Double, Integer>map=new HashMap<Double,Integer>();
		Comparator<Double>comp=new Comparator<Double>(){
			@Override
			public int compare(Double o1, Double o2) {
				return o1-o2;//MIN Heap
			}
		};
		PriorityQueue<Double> q=new PriorityQueue<Double>(nVideo,comp);//MIN heap to store top nVideo
		
		for (int i=1;i<nNode+1;i++){
			String vertex=g.IdentifyerToURL(i);	
			double score=hits.authorityScore(vertex);
			authority[i-1]=score;
			map.put(score,i-1);//map is used for look-up later
			
			if(q.size()<nVideo){
				q.add(score);
			}
			else{//q already has nVideo, we have to compare score with min_value, if score>min_value, then we remove min_value and add(score)
				if(q.peek()<score){
					q.remove();
					q.add(score);
				}
			}
		}//by this time we already have q with top nVideo, and map. Now we need to find indices of those top nVideo
		int indexRL=0;
		while(!q.isEmpty()){
			double v=q.poll();
			int index=map.get(v);
			String vertex=g.IdentifyerToURL(index+1);//this is a videoId
			recommendedList[indexRL++]=vertex;
		}
	}
}

public void printRecommendedList(){
	for(String videoId:recommendedList){
		String videoEntryUrl = "http://gdata.youtube.com/feeds/api/videos/"+videoId;
		try{
			VideoEntry videoEntry = service.getEntry(new URL(videoEntryUrl), VideoEntry.class);
			if (videoEntry.getTitle() != null) {
			      System.out.printf("Title: %s\n", videoEntry.getTitle().getPlainText());
			    }
			
		    YouTubeMediaGroup mediaGroup = videoEntry.getMediaGroup();
		    if(mediaGroup != null) {
		      MediaPlayer mediaPlayer = mediaGroup.getPlayer();
		      System.out.println(mediaPlayer.getUrl());
		    }
		}
		catch(Exception e){
			System.out.println(e.getMessage());
		}
	}
}

public void openURL(int index){
    String videoID=recommendedList[index];
		String videoEntryUrl = "http://gdata.youtube.com/feeds/api/videos/"+videoID;
		try{
		VideoEntry videoEntry = service.getEntry(new URL(videoEntryUrl), VideoEntry.class);
            YouTubeMediaGroup mediaGroup = videoEntry.getMediaGroup();
		    if(mediaGroup != null) {
		      MediaPlayer mediaPlayer = mediaGroup.getPlayer();
		      OpenURI.run(mediaPlayer.getUrl());
		    }
		}
		catch(Exception e){
			System.out.println(e.getMessage());
		}
	}
}

