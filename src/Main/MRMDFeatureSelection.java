package Main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class MRMDFeatureSelection {
	public static void main(String args[]) {
		
		// arff训练集文件名称
		//String TrainfeaName = "train.arff";
		String TrainfeaName = args[0];
		// arff测试集文件名称
		//String TestfeaName = "test.arff";
		String TestfeaName = args[1];
		// arff文件所在路径
		String dir = "";
		
		
		int initNum = 10; // 特征选择初始值
		initNum = Integer.parseInt(args[2]);
		int gap = 2; //特征选择间隔
		gap = Integer.parseInt(args[3]);
		int disFunc = 1; //MRMD参数
		int lableNum = 1; //MRMD参数
		int bestFeaNum = 0;
		double bestAccuracy = 0.0;

		String TrainInputFile = TrainfeaName;
		String TestInputFile = TestfeaName;
		int TraininsNum = new CaculateNum().getInstanceNum(TrainInputFile); // 训练集样本数
		int feaNum = new CaculateNum().getFeatureNum(TrainInputFile); // 样本总特征
		
		try {
			//从特征初始值开始，一次增加10个特征值实验
			for (int i = initNum; initNum <= i && i <= feaNum; i = i * gap)
			{
				System.out.print("当前特征维数：" + i);
				int seleFeaNum = i;
				
				// 调用MRMD.jar运行降维
				String TrainOutputFile = dir + TrainfeaName + "_temp" + ".txt";
				String command = "java -jar MRMD.jar -i " + TrainInputFile
						+ " -o " + TrainOutputFile + " -in " + TraininsNum
						+ " -fn " + feaNum + " -sn " + seleFeaNum + " -ln "
						+ lableNum + " -df " + disFunc + " -a " + dir
						+ TrainfeaName + "_temp" + ".arff";
				Process processTrain = Runtime.getRuntime().exec(command);
				processTrain.waitFor();

				//需要进行训练与测试的文件
				String trainFile = dir + TrainfeaName + "_temp" + ".arff";
				String testFile = dir + TestfeaName + "_temp" + ".arff";
				//将特征选择Train文件中的@语句，全部存入特征选择Test文件
				BufferedReader brarff = new BufferedReader(new FileReader(trainFile));
				BufferedWriter bw = new BufferedWriter(new FileWriter(testFile));
				String lString = null;
				while (brarff.ready()) {
					lString = brarff.readLine();
					if (lString.contains("data"))
						break;
					else {
						bw.write(lString);
						bw.newLine();
						bw.flush();
					}
				}
				bw.write(lString);
				bw.newLine();
				bw.flush();

				//从特征文件txt中提取特征标号存入整型数组feaRanked[]
				BufferedReader br = new BufferedReader(new FileReader(TrainOutputFile));
				br.readLine();
				br.readLine();
				br.readLine();
				br.readLine();
				br.readLine();
				br.readLine();
				String lString2 = null;
				String[] lString2Split = new String[3];
				int[] feaRanked = new int[seleFeaNum];
				int flag = 0;
				while (br.ready()) {
					lString2 = br.readLine();
					lString2Split = lString2.split("		");
					feaRanked[flag] = Integer.parseInt(lString2Split[1].substring(3, lString2Split[1].length())) + 1;
					flag++;
				}
				br.close();

				//依据上面得到的特征标号选取原始输入测试集，继续写入特征选择Test文件
				BufferedReader br2 = new BufferedReader(new FileReader(TestInputFile));
				String[] fea2split = new String[feaNum + 1];
				String lString3 = null;
				while (br2.ready()) {
					lString3 = br2.readLine();
					if (!lString3.contains("@") && lString3 != null) {
						fea2split = lString3.split(",");
						for (int j = 0; j < seleFeaNum; j++) {
							bw.write(fea2split[feaRanked[j] - 1] + ",");
						}
						bw.write(fea2split[feaNum]);
						bw.newLine();
						bw.flush();
					}
				}
				br2.close();
				bw.close();
				brarff.close();

				//开始进行Liblinear训练测试。输入：训练集，测试集，文件保存路径；输出：当前准确度，OutLiblinear临时文件
				double accuracy = new Liblinear().getLiblinear(dir, trainFile, testFile);
				
				//将最佳输出保存起来
				String flagMark;
				if (bestAccuracy < accuracy) {
					bestAccuracy = accuracy;
					bestFeaNum = seleFeaNum;
					
					new MRMDFeatureSelection().saveBest(
							trainFile, dir + TrainfeaName + "_best" + ".arff");
					new MRMDFeatureSelection().saveBest(
							testFile, dir + TestfeaName + "_best" + ".arff");
					new MRMDFeatureSelection().saveBest(
							dir + "OutLiblinear_temp"+ ".txt", dir + "OutLiblinear_best"+ ".txt");
					
					flagMark = "↑";
					System.out.print("本次分类准确度：" + accuracy + "%, ");
					System.out.println(flagMark);
				} else if (bestAccuracy == accuracy) {
					flagMark = "=";
					i = i / gap - 5;
					System.out.print("本次分类准确度：" + accuracy + "%, ");
					System.out.println(flagMark);
				} else {
					flagMark = "↓";
					System.out.print("本次分类准确度：" + accuracy + "%, ");
					System.out.println(flagMark);
					break;
				}
				
				
				
			}//一次特征选择分类过程结束
			
			System.out.println("\n" + "最佳分类准确度：" + bestAccuracy + "%");
			System.out.println("最佳特征维数：" + bestFeaNum);
			
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			System.exit(0);
		}
				
	}
	
	public void saveBest(String currentFile, String bestFile) {
		try {
			String temp;
			BufferedReader br = new BufferedReader(new FileReader(currentFile));
			BufferedWriter bf = new BufferedWriter(new FileWriter(bestFile));
			temp = br.readLine();
			bf.write(temp);
			while (br.ready()) {
				temp = br.readLine();
				bf.write("\n" + temp);
			}
			br.close();
			bf.close();
		} catch (Exception e) {
			System.out.println("ERROR!");
		}
	}
	
}
