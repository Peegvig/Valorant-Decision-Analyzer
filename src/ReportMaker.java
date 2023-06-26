import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import javax.imageio.ImageIO;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber.Exception;
import org.bytedeco.javacv.Java2DFrameConverter;
import javax.swing.*;
/*CURRENT PROBLEMS :
 * TAKES WAY TOO LONG. 15+ mins for first time of getFrames()/getMovementErrorFrames()
 * sometimes findindexframe1offight() method works wrong or smth... maybe not? maybe error in input param?!?/
 * need to decrease amt of frames by a LOT. like check 1 in 30 for gun, if not, then skip next 29. only get enemy for last ~10 seconds.
 * getMaxFrames() takes 4 seconds to get output
 * reversing takes 11 seconds (worth probably) but 5 second cropping is 1 second.
 * enemy finding for 5 seconds takes 20 seconds PROBLEM!!!!
 */
public class ReportMaker { //when losslesscut says frame 6996, it is 6997 in yolov5 labels, x y width height
	public static int sample = 2;
	public static int fps = 30;
	private static String videoPath = "SampleData/Sample"+sample+"/Test2Vid.mp4";
	private static String absoluteVideoPath = "C:\\Users\\lahiv\\eclipse-workspace\\VGA\\SampleData\\Sample"+sample+"\\Test2Vid.mp4";
	private static String croppedVideoPath = "C:\\Users\\lahiv\\eclipse-workspace\\VGA\\SampleData\\Sample"+sample+"\\Test2VidReversed_00_5Reversed.mp4";
	private static File[] enemyFiles;
	private static File[] gunFiles;
	public static File[] frameFiles;
	public static String frameFilesName;
	public static File[] movementErrorFiles;
	public static String movementErrorFilesName;
	public static String movementErrorVideoName;
	private static int frameCount;
	private static int framesBeforeEnd = 100;
	private static String issues  = "";
  	public static void main(String[] args) throws IOException, InterruptedException { 

		long time = System.currentTimeMillis();
		
		frameCount=getMaxFrames();//4 seconds
		framesBeforeEnd=frameCount-framesBeforeEnd-frameCount%(fps/10);
		System.out.println(System.currentTimeMillis()-time+"ms - Frame Count Obtained.");
		time = System.currentTimeMillis();
		
		//populateSample(); //56 seconds...
		System.out.println(System.currentTimeMillis()-time+"ms - Sample Populated.");
		time = System.currentTimeMillis();
		
		enemyFiles = new File("SampleData/Sample"+sample+"/Enemies/exp/labels").listFiles();
		gunFiles = new File("SampleData/Sample"+sample+"/Guns/exp/labels").listFiles();
		frameFiles = new File("SampleData/Sample"+sample+"/Frames").listFiles();
		movementErrorFiles = new File("SampleData/Sample"+sample+"/MovementError").listFiles();
		sortFolders();

		System.out.print("First Shot Accurate? :: " + isFirstShotAccurate(getFileNum(getFile(enemyFiles,findIndexFrame1OfFight(framesBeforeEnd)))));
		System.out.println(" - "+(System.currentTimeMillis()-time)+"ms");
		time = System.currentTimeMillis();

		System.out.print("Current Gun at Start of Fight :: " + getLastEquippedGun(0));
		System.out.println(" - "+(System.currentTimeMillis()-time)+"ms");
		//System.out.println("enemyFiles " + enemyFiles[findIndexFrame1OfFight(framesBeforeEnd)]);
		System.out.print("Frame 1 of Fight :: " +  getFileNum(enemyFiles[findIndexFrame1OfFight(framesBeforeEnd)]));
		System.out.println(" - "+(System.currentTimeMillis()-time)+"ms");
		time = System.currentTimeMillis();
		System.out.print("Distance of Enemy at Start of Fight :: " + getDistanceOfEnemy(framesBeforeEnd));
		System.out.println(" - "+(System.currentTimeMillis()-time)+"ms");
		time = System.currentTimeMillis();
		System.out.print("Crosshair Placement at Start of Fight :: " + getCrosshairPlacement(findIndexFrame1OfFight(framesBeforeEnd)));
		System.out.println(" - "+(System.currentTimeMillis()-time)+"ms");
		time = System.currentTimeMillis();
		System.out.print("Millisecods From Enemy Found and First Shot :: " + msFromEnemyFoundToFirstShot(getFileNum(enemyFiles[findIndexFrame1OfFight(framesBeforeEnd)])));
		System.out.println(" - "+(System.currentTimeMillis()-time)+"ms");
		time = System.currentTimeMillis();//LONG TIME
		System.out.println(issues);
	}
	public static void sortFolders() {
        Arrays.sort(enemyFiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Integer.valueOf(getFileNum(f1)).compareTo(getFileNum(f2));
            }
        });
        Arrays.sort(gunFiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Integer.valueOf(getFileNum(f1)).compareTo(getFileNum(f2));
            }
        });
//        Arrays.sort(frameFiles, new Comparator<File>() {
//            public int compare(File f1, File f2) {
//                return Integer.valueOf(getFileNum(f1)).compareTo(getFileNum(f2));
//            }
//        });
        Arrays.sort(movementErrorFiles, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return Integer.valueOf(getFileNum(f1)).compareTo(getFileNum(f2));
            }
        });
	}
	public static int getMaxFrames() throws IOException {
		Process frames = Runtime.getRuntime().exec("cmd /c ffprobe.exe -v error -select_streams v:0 -count_frames -show_entries stream=nb_read_frames -of default=nokey=1:noprint_wrappers=1 " + croppedVideoPath,null, new File("C:\\FFmpeg\\bin\\"));
		BufferedReader output_reader = new BufferedReader(new InputStreamReader(frames.getInputStream()));
        String output = "";
        while ((output = output_reader.readLine()) != null) {
            return Integer.parseInt(output);
        }
        return -1;
	}
	public static int getIndex(File[] folder,int frameNumber) {
		for (int i = 0; i<folder.length;i++) {
			if (getFileNum(folder[i])==frameNumber) {
				return i;
			}
		}
		return -1;
	}
	public static String getLastEquippedGun(int framesBeforeEnd) throws FileNotFoundException {
		int frame=getFileNum(gunFiles[gunFiles.length-1])-framesBeforeEnd;
		int lastFrameWithGunBeforeRequest = -1;
		int indexOflastFrameWithGunbeforeRequest = -1;
		for(int i = gunFiles.length-1; i>=0; i--) {
			if (getFileNum(gunFiles[i])<=frame) {
				lastFrameWithGunBeforeRequest = getFileNum(gunFiles[i]);///hhmmmmmgggg
				//lastFrameWithGunBeforeRequest = getFileNum(new File(gunLabelName+"_"+i+".txt"));
				indexOflastFrameWithGunbeforeRequest = i;
				break;
			}
		}
		String[] gunNames = {"Ares", "Bucky", "Bulldog", "Classic", "Frenzy", "Ghost", "Guardian", "Judge", "Marshall", "Odin", "Operator", "Phantom", "Sheriff", "Shorty", "Spectre", "Stinger", "Vandal"};
		Scanner sc = new Scanner(gunFiles[indexOflastFrameWithGunbeforeRequest]);
		String gunName = gunNames[sc.nextInt()];
		if (indexOflastFrameWithGunbeforeRequest>12) {
			int score = 0;
			for (int i = 0; i<=12; i++) {
				if(getFileNum(gunFiles[indexOflastFrameWithGunbeforeRequest-i])==lastFrameWithGunBeforeRequest-i && 
				new Scanner(gunFiles[indexOflastFrameWithGunbeforeRequest]).nextInt() == new Scanner(gunFiles[indexOflastFrameWithGunbeforeRequest-i]).nextInt()) {
					score++;
				}
			}
			if (score>9) {
				sc.close();
				return gunName; 
			}
			else{
				sc.close();
				return getLastEquippedGun(framesBeforeEnd+15);
			}
		}		
		else {
			sc.close();
			return "nO gUN";
		}
	}
	public static int findIndexFrame1OfFight(int frame) throws FileNotFoundException {
		if (frame>frameCount) {
			return -1000;
		}
		int indexLastFrame = enemyFiles.length-1;
//		for(int i = 0; i<enemyFiles.length+1;i++) {
//			if(i==enemyFiles.length) {
//				return -2;
//			}
//			int fileNum=getFileNum(enemyFiles[i]);
//			System.out.println("fileNum " + fileNum);
//			if (fileNum>frame) {
//				indexLastFrame = i;
//				break;
//			}
		//}
		boolean found = false;
		while(indexLastFrame>3) {
			if( (hasEnemyHead(enemyFiles[indexLastFrame])&&
				 hasEnemyHead(enemyFiles[indexLastFrame-1])&&
				 hasEnemyHead(enemyFiles[indexLastFrame-2]))&&
				 getFileNum(enemyFiles[indexLastFrame-1])==getFileNum(enemyFiles[indexLastFrame])-1 &&
				 getFileNum(enemyFiles[indexLastFrame-2])==getFileNum(enemyFiles[indexLastFrame])-2) {
					found=true;
			}
			if(	 found && (!hasEnemyHead(enemyFiles[indexLastFrame])&&
				  !hasEnemyHead(enemyFiles[indexLastFrame-1])&&
				  !hasEnemyHead(enemyFiles[indexLastFrame-2]))) {
					return indexLastFrame;
				}
			indexLastFrame--;
		}
		return indexLastFrame;
		
	}
	public static String getDistanceOfEnemy(int frame) throws FileNotFoundException { //must be 3/3 for fight
		//frame 846 is a mid range fight (1 0.533984 0.486111 0.00546875 0.0166667) x y width height
		//frame 4013 is mid range fight (1 0.499609 0.486806 0.00546875 0.0180556)
		//frame 7103 is a mid range fight (1 0.492578 0.451389 0.00546875 0.0138889)
		//frame 6997 is a close range fight (1 0.465234 0.506944 0.00703125 0.025)
		//frame 2427 is a close range fight (1 0.549219 0.443056 0.00625 0.025)
		//0.019 is cutoff???
		//need to find first frame of fight, when enemy head appears for 3 consecutive frames,
		//get frame, go to start of fight by going backwards file by file until the first time there is 7/10 consecutive frames of enemy head
		int firstIndexFrameNum = findIndexFrame1OfFight(frame);
		if ( firstIndexFrameNum<0) {
			return "firstIndexFrameNum :: " + firstIndexFrameNum;
		}
		while (true&&firstIndexFrameNum+3<enemyFiles.length) {
//			if (getFileNum(enemyFiles[firstIndexFrameNum])>frame && orig>2) {
//				System.out.println("NYASAYAYY : " + getFileNum(enemyFiles[orig-1]));
//				firstIndexFrameNum = findIndexFrame1OfFight(getFileNum(enemyFiles[orig-1]));
//				orig=firstIndexFrameNum;
//			}
			if(hasEnemyHead(enemyFiles[firstIndexFrameNum]) && hasEnemyHead(enemyFiles[firstIndexFrameNum+1]) && hasEnemyHead(enemyFiles[firstIndexFrameNum+2])){
				double f1H = getHeight(enemyFiles[firstIndexFrameNum]);
				double f2H = getHeight(enemyFiles[firstIndexFrameNum+1]);
				double f3H = getHeight(enemyFiles[firstIndexFrameNum+2]);
				if (Math.abs(f1H-f2H)<0.005 && Math.abs(f2H-f3H)<0.005 && Math.abs(f1H-f3H)<0.005) {
					System.out.print(enemyFiles[firstIndexFrameNum].getName() + " ");
					if ((f1H+f2H+f3H)/3 > .019) {
						return "Close Range ";
					}
					else if ((f1H+f2H+f3H)/3 < .019 && (f1H+f2H+f3H)/3 > .010) {
						return "Mid Range ";
					}
					else {
						return "Long Range ";
					}
				}
				
			}
			//System.out.println("3 frames dont all have head " + enemyFiles[firstIndexFrameNum].getName());
			firstIndexFrameNum+=3;
		}
		return "No Fighrs??!?!/";
	}
	private static double getHeight(File f) throws FileNotFoundException {
		Scanner sc = new Scanner(f);
		if(hasEnemyHead(f)) {
			while(sc.hasNextLine()) {
				if (sc.nextInt()==1) {
					sc.next(); sc.next(); sc.next();
					sc.close();
					return sc.nextDouble();
				}
				sc.nextLine();
			}
		}
		else{
			sc.close();
			return -2.0;
		}
		sc.close();
		return -1.0;
	}
	private static File getFile(File[] folder, int num) {
		for(int i = 0; i<folder.length;i++) {
			if(getFileNum(folder[i])==num) {
				return folder[i];
			}
			if(getFileNum(folder[i])>num && i>0) {
				return folder[i-1];
			}
		}
		return null;
	}
	private static int getFileNum(File f) {
		return Integer.parseInt(f.getName().substring(f.getName().lastIndexOf("_")+1,f.getName().indexOf(".")));
	}
	private static boolean hasEnemyHead(File f) throws FileNotFoundException {

		Scanner sc = new Scanner(f);
		while(sc.hasNextLine()) {
			if (sc.nextInt()==1) {
				sc.close();
				return true;
			}
			sc.nextLine();
		}
		sc.close();
		return false;
	}
	private static String getCrosshairPlacement(int index) throws FileNotFoundException {
		String output = "";
		double x = -1;
		double y = -1;
		double width = -1;
		double height = -1;
		while(!hasEnemyHead(enemyFiles[index])) {
			index+=1;
		}
		//Scanner sc = new Scanner(enemyFiles[index]);
		Scanner sc = new Scanner(enemyFiles[index]);
		while(sc.hasNext()) {
			if(sc.nextInt()==1) {
				x=sc.nextDouble();
				y=sc.nextDouble();
				width=sc.nextDouble();
				height=sc.nextDouble();
			}
			sc.nextLine();
		}
		sc.close();
		if (x==-1 || y==-1 || width==-1 || height ==-1) {
			return "ERROR";
		}
		if (.45>x+width && .45>x) {
			output+="RIGHT ";
		}
		if (.35>x+width && .35>x) {
			issues +="SUPER FAR RIGHT CROSSHAIR PLACEMENT! YOU WERE PROBABLY NOT LOOKING AT THE ANGLE THE ENEMY SWUNG.\n";
		}
		if (.55<x+width && .55<x) {
				output+="LEFT ";
		}
		if (.65<x+width && .65<x) {
			issues +="SUPER FAR LEFT CROSSHAIR PLACEMENT! YOU WERE PROBABLY NOT LOOKING AT THE ANGLE THE ENEMY SWUNG.\n";
		}
		if (.45>y+height && .45>y) {
			output+="LOW ";
		}
		if (.55<y+height && .55<y) {
			output+="HIGH ";
		}
		if (output.length()==0) {
			output+="IN ENEMY HEAD! ";
		}
		
		output += "(" +x + ", " + (x+width) + ")" + "(" +y + ", " + (y+height) + ")";
		System.out.print(enemyFiles[index].getName() + " ");
		return output;
	}
	private static void getFrames() throws Exception, IOException {
		if (frameFiles.length<frameCount) {
			FFmpegFrameGrabber g = new FFmpegFrameGrabber(videoPath);
			g.start();
	
			for (int i = frameCount-5*fps ; i < frameCount+1; i=i+fps/6) {
				g.setFrameNumber(i);
				ImageIO.write(new Java2DFrameConverter().convert(g.grab()), "png", new File(frameFilesName+"/frame_" + i + ".png"));
				System.out.println("F " + i);
			}
			g.stop();
	        g.close();
		}
	}
	private static void getMovementErrorFrames() throws Exception, IOException {	
		File folder = new File(movementErrorFilesName);
		folder.mkdir();
		FFmpegFrameGrabber g = new FFmpegFrameGrabber(movementErrorVideoName);
		g.start();

		for (int i = 0 ; i < fps*5; i=i+fps/10) {
			g.setFrameNumber(i);
			
			ImageIO.write(new Java2DFrameConverter().convert(g.grab()), "png", new File(movementErrorFilesName+"/frame_" + i + ".png"));
			//System.out.println("ME " + i);
		}
		g.stop();
        g.close();
	}
	private static void displayFrame(int frameNumber) {
		JFrame jframe = new JFrame();
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JLabel jlabel = new JLabel();
		jlabel.setIcon(new ImageIcon(frameFilesName+"/frame_"+frameNumber+".png"));
		//System.out.println(frameFilesName+"/frame_"+frameNumber+".png");
		jframe.add(jlabel);
		jframe.setSize(1290, 760);
		jframe.setVisible(true);
	}
	private static void displayMovementErrorFrame(int frameNumber) {
		JFrame jframe = new JFrame();
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JLabel jlabel = new JLabel();
		jlabel.setIcon(new ImageIcon(movementErrorFilesName+"/frame_"+frameNumber+".png"));
		//System.out.println(movementErrorFilesName+"/frame"+frameNumber+".png");
		jframe.add(jlabel);
		jframe.setSize(100, 80);
		jframe.setVisible(true);
	}
	private static boolean hasMovementError(int index) throws IOException {
		File file = movementErrorFiles[index];
		BufferedImage bimg = ImageIO.read(file);
		int width = bimg.getWidth();
		int height = bimg.getHeight();
		int movePixel = 0;
		for (int x = 75; x<width; x++) {
			for (int y = 0; y<height; y++) {
				int[] pxColor = new int[3];
				pxColor=getPixel(file,x,y);
				if ((pxColor[0]>0 && pxColor[0]<171)&&(pxColor[1]>224 && pxColor[1]<256)&&(pxColor[2]>219 && pxColor[2]<256)) {
					movePixel++;
				}
				if(movePixel>=8) {
					return true;
				}
			}
		}
		//displayMovementErrorFrame(frameNumber);
		return false;
	}
	private static boolean hasAccurateShot(int index) throws IOException {
		File file = movementErrorFiles[index];
		BufferedImage bimg = ImageIO.read(file);
		int width = bimg.getWidth();
		int height = bimg.getHeight();
		int shotPixel = 0;
		for (int x = 75; x<width; x++) {
			for (int y = 0; y<height; y++) {
				int[] pxColor = new int[3];
				pxColor=getPixel(file,x,y);
				if ((pxColor[0]>180 && pxColor[0]<255)&&(pxColor[1]>160 && pxColor[1]<205)&&(pxColor[2]>22 && pxColor[2]<141)) {
					shotPixel++;
				}
			}
		}
		//displayMovementErrorFrame(frameNumber);
		if(shotPixel>=30) {
			return true;
		}
		return false;
	}
	private static int msFromEnemyFoundToFirstShot(int frameNumber) throws IOException {
		//PUT A getFileNum(enemyFiles[findIndexFrame1OfFight(frame)])) RESULT FOR THE PARAMETER
		int currentFrame = frameNumber;
		boolean firstShotFound = false;
		while(getIndex(movementErrorFiles,currentFrame)==-1) {
			currentFrame++;
		}
		while(!firstShotFound) {
			if (currentFrame>=frameCount) {
				return -3;
			}
			if (hasAccurateShot(getIndex(movementErrorFiles,currentFrame)) || hasMovementError(getIndex(movementErrorFiles,currentFrame))) {
				firstShotFound=true;
				break;
			}
			currentFrame+=fps/10;
		}
		int ms = (int) (((currentFrame-frameNumber)*1000/fps)/1000.0*1000);
		if (ms>600) {
			issues+="VERY SLOW TO FIRE AFTER FINDING ENEMY!\n";
		}
		return ms;
	}
	private static boolean isFirstShotAccurate(int frameNumber) throws IOException {
		//PUT A getFileNum(enemyFiles[findIndexFrame1OfFight(frame)])) RESULT FOR THE PARAMETER
		int currentFrame=frameNumber;
		while (currentFrame%fps/10!=0) {
			currentFrame++;
		}
		while(true) {
			if (hasMovementError(getIndex(movementErrorFiles,currentFrame))) {
				return false;
			}
			if(hasAccurateShot(getIndex(movementErrorFiles,currentFrame))) {
				return true;
			}
			currentFrame=currentFrame+fps/10;
		}
	}
	public static int[] getPixel(File f,int x, int y) throws IOException {
        int[] RGB = new int[3];
        try {
        // Load image from file
        BufferedImage image = ImageIO.read(f);
        // Get pixel color at (x,y) coordinates
        int pixel = image.getRGB(x, y);
        // Extract RGB values from pixel
        RGB[0] = (pixel >> 16) & 0xff;
        RGB[1] = (pixel >> 8) & 0xff;
        RGB[2] = pixel & 0xff;
        }
        catch (Exception e){
            System.out.println("ERROR READING IMG IN getPixel()!!!");
                    
        }
        return RGB;
    }
	public static void populateSample() throws IOException, InterruptedException { // very much hard coded for specific directories
		//have enemies, guns, original vid
		//need to populate frames, mvmt error, mvmt error vid(?), gun cropped vid(?)
		//think last 2 are only used for debugging so maybe not them.
		//take vid -> output full folder
		
		//vid -> yolov5 enemy -> enemies folder/vid
		// |-> gun crop -> yolov5 gun -> guns folder/vid
		// |-> mvmt crop -> parse frames -> mvmt folder/vid (DONE IN getMovementErrorFrames())!
		// |-> parse frames -> frames folder (DONE IN getFrames())!
		
		//need cmds for crop and yolov5.
		long time = System.currentTimeMillis();
		
		movementErrorFilesName = "SampleData/Sample"+sample+"/MovementError";
		movementErrorVideoName = "C:\\Users\\lahiv\\eclipse-workspace\\VGA\\SampleData\\Sample"+sample+"\\Test2VidMvmtCropReversed_00_5Reversed.mp4";
		frameFilesName = "SampleData/Sample"+sample+"/Frames";
		reverseVideo(absoluteVideoPath);
		cropVideo(absoluteVideoPath.substring(0,absoluteVideoPath.length()-4)+"Reversed.mp4","00:00:00","5");
		String lastFiveSeconds = absoluteVideoPath.substring(0,absoluteVideoPath.length()-4)+"Reversed"+"_00_"+"5.mp4";
		reverseVideo(lastFiveSeconds);
		lastFiveSeconds=lastFiveSeconds.substring(0,lastFiveSeconds.length()-4)+"Reversed.mp4";
		System.out.println(System.currentTimeMillis()-time+" - ms. Last 5 Seconds Video Made.");
		time = System.currentTimeMillis();
		
		deleteFolder("C:\\Users\\lahiv\\eclipse-workspace\\VGA\\SampleData\\Sample"+sample+"\\Enemies\\exp");
		runCondaCommand("python C:\\Users\\lahiv\\Repos\\yolov5\\detect.py --conf 0.7 --save-txt --project C:\\Users\\lahiv\\eclipse-workspace\\VGA\\SampleData\\Sample"+sample+"\\Enemies --source " + lastFiveSeconds + " --weights " + "C:\\Users\\lahiv\\OneDrive\\ValorantProjectStuff\\weights\\EnemyFinderWeightsEpoch100.pt");
		//enemies folder maker. takes 20 seconds for 5 seconds. 
		
		System.out.println(System.currentTimeMillis()-time+" - ms. Last 5 Seconds Enemies Yolov5 Folder Made.");
		time = System.currentTimeMillis();
		
		File f = new File(absoluteVideoPath.substring(0,absoluteVideoPath.length()-4)+"GunCrop.mp4");
		if (f.exists()) {
			f.delete();
		}
		String gunVideo = absoluteVideoPath.substring(0,absoluteVideoPath.length()-4)+"GunCrop.mp4";
		runCondaCommand("ffmpeg.exe -i "+absoluteVideoPath+" -vf \"crop=141:108:1088:504\" "+gunVideo);
		reverseVideo(gunVideo);
		cropVideo(gunVideo.substring(0,gunVideo.length()-4)+"Reversed.mp4","00:00:00","30");
		String lastThirtySecondsGun = gunVideo.substring(0,gunVideo.length()-4)+"Reversed"+"_00_"+"30.mp4";
		reverseVideo(lastThirtySecondsGun);
		lastThirtySecondsGun=lastThirtySecondsGun.substring(0,lastThirtySecondsGun.length()-4)+"Reversed.mp4";
		
		System.out.println(System.currentTimeMillis()-time+" - ms. Last 30 Seconds Gun Video Made.");
		time = System.currentTimeMillis();
		
		deleteFolder("C:\\Users\\lahiv\\eclipse-workspace\\VGA\\SampleData\\Sample"+sample+"\\Guns\\exp");
		runCondaCommand("python C:\\Users\\lahiv\\Repos\\yolov5\\detect.py --conf 0.7 --save-txt --project C:\\Users\\lahiv\\eclipse-workspace\\VGA\\SampleData\\Sample"+sample+"\\Guns --source " + lastThirtySecondsGun +  " --weights " + "C:\\Users\\lahiv\\OneDrive\\ValorantProjectStuff\\weights\\PVGA1epoch100.pt");
		//guns folder maker. takes 20 seconds for 5 seconds. BRUH
		
		System.out.println(System.currentTimeMillis()-time+" - ms. Last 30 Seconds Gun Yolov5 Folder Made.");
		time = System.currentTimeMillis();
		
		
//		f = new File(absoluteVideoPath.substring(0,absoluteVideoPath.length()-4)+"MvmtCrop.mp4");
//		if (f.exists()) {
//			f.delete();
//		}
		String mvmtVideo=absoluteVideoPath.substring(0,absoluteVideoPath.length()-4)+"MvmtCrop.mp4";
		runCondaCommand("ffmpeg.exe -i "+absoluteVideoPath+" -vf \"crop=80:30:1174:154\" "+mvmtVideo);
		reverseVideo(mvmtVideo);
		String reversedMvmtVideo = mvmtVideo.substring(0,mvmtVideo.length()-4)+"Reversed.mp4";
		cropVideo(reversedMvmtVideo,"00:00:00","5");
		String lastFiveMvmtSeconds = mvmtVideo.substring(0,mvmtVideo.length()-4)+"Reversed"+"_00_"+"5.mp4";
		reverseVideo(lastFiveMvmtSeconds);
		deleteFolder("C:\\Users\\lahiv\\eclipse-workspace\\VGA\\SampleData\\Sample"+sample+"\\MovementError");
		
		System.out.println(System.currentTimeMillis()-time+" - ms. Last 5 Seconds Movement Error Video Made.");
		time = System.currentTimeMillis();
		
		getMovementErrorFrames();
		
		System.out.println(System.currentTimeMillis()-time+" - ms. Movement Error Frames Obtained.");
		time = System.currentTimeMillis();
		
		ArrayList<File> unusedVidsToDelete = new ArrayList<File>();
		unusedVidsToDelete.add(new File("SampleData/Sample"+sample+"/Test2VidGunCrop.mp4"));
		unusedVidsToDelete.add(new File("SampleData/Sample"+sample+"/Test2VidGunCropReversed.mp4"));
		unusedVidsToDelete.add(new File("SampleData/Sample"+sample+"/Test2VidGunCropReversed_00_30.mp4"));
		unusedVidsToDelete.add(new File("SampleData/Sample"+sample+"/Test2VidMvmtCrop.mp4"));
		unusedVidsToDelete.add(new File("SampleData/Sample"+sample+"/Test2VidMvmtCropReversed.mp4"));
		unusedVidsToDelete.add(new File("SampleData/Sample"+sample+"/Test2VidMvmtCropReversed_00_5.mp4"));
		unusedVidsToDelete.add(new File("SampleData/Sample"+sample+"/Test2VidReversed.mp4"));
		unusedVidsToDelete.add(new File("SampleData/Sample"+sample+"/Test2VidReversed_00_5.mp4"));
		for (File file: unusedVidsToDelete) {
			if (file.exists()) {
				file.delete();
			}
		}
	}
    public static void deleteFolder(String Absdir) {
		try {
            // Create a Path object representing the folder
            Path folder = Paths.get(Absdir);

            // Delete the folder and its contents recursively
            File f = new File(Absdir);
            if (f.exists()) {
	            Files.walk(folder)
	                    .sorted((p1, p2) -> -p1.compareTo(p2)) // Sort in reverse order to delete inner files first
	                    .forEach(path -> {
	                        try {
	                            Files.delete(path);
	                        } catch (IOException e) {
	                            e.printStackTrace();
	                        }
	                    });
	
            }
        } 
		catch (IOException e) {
            e.printStackTrace();
        }
    }
	public static String runCondaCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("conda", "run", "-n", "yolov5", "cmd.exe", "/C", command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder(); 
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n"); 
           // System.out.println(line);
        }
        StringBuilder errorMessage = new StringBuilder(); 
        BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        while ((line = errorReader.readLine()) != null) {
        	errorMessage.append(line).append("\n");
        }
      //  System.out.print(errorMessage);
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Conda command execution failed. " + exitCode);
        }

        return command+" executed.";
    }
    public static void reverseVideo(String filePath) throws IOException, InterruptedException {
    	File f = new File(filePath.substring(0,filePath.length()-4)+"Reversed.mp4");
    	if (f.exists()) {
    		f.delete();
    	}
    	ProcessBuilder pB = new ProcessBuilder("ffmpeg","-i",filePath,"-vf","reverse","-an",filePath.substring(0,filePath.length()-4)+"Reversed.mp4");
    	Process p = pB.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		//String line;
       // while ((line = reader.readLine()) != null) {
            //System.out.println(line);
       // }
        reader.close();

        int exitCode = p.waitFor();
        if (exitCode == 0) {
        } else {
            System.out.println("Failed to reverse the video.");
        }
    }
    public static void cropVideo(String cropAbsVidpath, String startTime, String duration) throws IOException, InterruptedException {
    	File f = new File(cropAbsVidpath.substring(0,cropAbsVidpath.length()-4)+"_"+startTime.substring(startTime.length()-2)+"_"+duration+".mp4");
    	if (f.exists()) {
    		f.delete();
    	}
    	
    	ProcessBuilder pB = new ProcessBuilder("ffmpeg","-i",cropAbsVidpath,"-ss",startTime,"-t",duration,cropAbsVidpath.substring(0,cropAbsVidpath.length()-4)+"_"+startTime.substring(startTime.length()-2)+"_"+duration+".mp4");
    	Process p = pB.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        //String line;
        //while ((line = reader.readLine()) != null) {
        //    System.out.println(line);
        //}
        reader.close();

        int exitCode = p.waitFor();
        if (exitCode == 0) {
        } else {
            System.out.println("Failed to crop the video.");
        }
    }
}


