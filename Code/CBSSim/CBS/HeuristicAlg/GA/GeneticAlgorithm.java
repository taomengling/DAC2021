package org.cloudbus.cloudsim.examples.CBS.HeuristicAlg.GA;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

public abstract class GeneticAlgorithm {
    // 适应度 越大越好，下一版改成成本越小越好
    private List<Chromosome> population = new ArrayList<>(); //种群列表
    private int popSize = 2000;//种群数量
    private int geneSize = 0 ;//基因最大长度
    private int generange = 0;// 基因取值范围
    private int maxIterNum = 500;//最大迭代次数
    private double crossRate = 0.6;
    private double mutationRate = 0.03;//基因变异的概率
    private int maxMutationNum = 3;//最大变异次数

    private int generation = 1;//当前遗传到第几代

    private double bestScore;//最好得分  局部
    private double worstScore;//最坏得分  局部
    private double totalScore;//总得分  局部
    private double averageScore;//平均得分
    private Chromosome best_chromosome;
    private int[] x; //记录历史种群中最好的X值
    private double y; //记录历史种群中最好的Y值
    private int geneI;//x y所在代数
    private String FitTarget ;

    public GeneticAlgorithm(int cd_size,int wh_size) {
        this.geneSize = cd_size;
        this.generange = wh_size;
        this.FitTarget = "qos";
    }

    public int[] calculate() {
        generation = 1;
        init();
        while (generation < maxIterNum) {  //迭代maxIterNum
            evolve();   // （选择 -> 交叉)+ -> 变异 -> 计算得分
            print();    // 打印
            generation++;  // 代数
        }
        return best_chromosome.getGene();
    }

    private void evolve() {
        List<Chromosome> childPopulation = new ArrayList<>();
        while (childPopulation.size() < popSize) {
            Chromosome p1 = getParentChromosome();
            Chromosome p2 = getParentChromosome();
            List<Chromosome> chromosomes = Chromosome.genetic(p1, p2, crossRate);
            if (chromosomes != null)
                childPopulation.addAll(chromosomes);
        }
        List<Chromosome> t = population;
        population = childPopulation;
        t.clear();
        t = null;
        mutation();
        try{
            calculateScore();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    /**
     * 初始化种群
     */
    private void init()  {
        population = new ArrayList<>();
        for (int i = 0; i < popSize; i++) {
            Chromosome chromosome = new Chromosome(geneSize,generange);
            population.add(chromosome);
        }
        try{
             calculateScore();
        }catch (InterruptedException e){
            e.printStackTrace();
        }

    }

    /**
     * 选择过程:轮盘赌法
     * @return
     */
    private Chromosome getParentChromosome() {
        double slide = Math.random() * totalScore;
        if (slide==0){ //所有解的适应度都为0
            return population.get((int)(Math.random()*population.size()));
        }
        double sum = 0;
        for (Chromosome chromosome : population) {
            sum += chromosome.getScore();
            if (slide < sum && chromosome.getScore() >= averageScore)
                return chromosome;
        }
        return null;
    }
    private class calculateGroupScore extends Thread{
        public List<Chromosome> group;
        public int id;
        public calculateGroupScore(List<Chromosome> group,int id){
            this.group = group;
            this.id = id;
        }
        public synchronized void calculate() throws InterruptedException{
            for(Chromosome ch:this.group) {
                setChromosomeScore(ch);
            }
        }
        @Override
        public void run(){
            super.run();
            try {
                calculate();
                System.out.println("线程" + id + "is running!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    /**
     * 计算所有个体的适应度(并行化)
     */
    private void calculateScore() throws InterruptedException { //计算适应度 越大越好
        //第一个个体的得分
        int count = 0;
        setChromosomeScore(population.get(0));
        bestScore = population.get(0).getScore();
        worstScore = population.get(0).getScore();
        totalScore = 0;
        best_chromosome = population.get(0);
        x = changeX(population.get(0));
        y = bestScore;
        int thread_num = popSize/100;
        List<calculateGroupScore> thread_list = new ArrayList<>();
        for(int i=0;i<thread_num;i++){
            calculateGroupScore cal_thread =  new calculateGroupScore(population.subList(i*100,i*100+100),i+1);
            thread_list.add(cal_thread);
            cal_thread.start();

        }
        for(int i=0;i<thread_num;i++){
            thread_list.get(i).join();
        }
//        for (Chromosome chromosome : population) { //遍历所有个体得到得分
//            setChromosomeScore(chromosome);
//            if (chromosome.getScore() > bestScore){ //更新本次迭代时的最优个体
//                bestScore = chromosome.getScore();
//                if (y > bestScore) { //更新全局最优解
//                    setBest_chromosome(chromosome);
//                    x = changeX(chromosome);
//                    y = bestScore;
//                    geneI = geneSize;
//                }
//            }
//            if (chromosome.getScore() < worstScore)
//                worstScore = chromosome.getScore();
//            totalScore += chromosome.getScore();
//            count ++ ;
//        }
        for (Chromosome chromosome : population) { //遍历所有个体得到得分
            if (chromosome.getScore() > bestScore){ //更新本次迭代时的最优个体
                bestScore = chromosome.getScore();
                if (y < bestScore) { //更新全局最优解
                    x = changeX(chromosome);
                    y = bestScore;
                    best_chromosome = chromosome;
                    geneI = geneSize;
                }
            }
            if (chromosome.getScore() < worstScore)
                worstScore = chromosome.getScore();
            totalScore += chromosome.getScore();
            count ++ ;
        }
        averageScore = totalScore / popSize;
        averageScore = Math.min(averageScore, bestScore);
        if (averageScore>1/(0.05 * 2 * this.generange)){
            setFitTarget("cost");
        }
    }

    private void mutation() {
        for (Chromosome chromosome : population) {
            if (Math.random() < mutationRate)
                chromosome.mutation((int) (Math.random() * maxMutationNum));   //变异次数

        }
    }

    private void setChromosomeScore(Chromosome chromosome) {
        if (chromosome == null) {
            return;
        }
        int[] x = changeX(chromosome);
        double y = calculateY(x);
        chromosome.setScore(y);
    }

    private void print() {
        System.out.println("--------------------------------");
        System.out.println("the generation is:" + generation);
        System.out.println("the best y is:" + bestScore);
        System.out.println("the worst fitness is:" + worstScore);
        System.out.println("the average fitness is:" + averageScore);
        System.out.println("the total fitness is:" + totalScore);
        System.out.println("geneI:" + geneI + "\tx:" + x.toString() + "\ty:" + y);
        System.out.println(getBest_chromosome().toString());
    }

    public abstract double calculateY(int[] map_array);

    public abstract int[] changeX(Chromosome chromosome);

    public List<Chromosome> getPopulation() {
        return population;
    }

    public void setPopulation(List<Chromosome> population) {
        this.population = population;
    }

    public int getPopSize() {
        return popSize;
    }

    public void setPopSize(int popSize) {
        this.popSize = popSize;
    }

    public int getGeneSize() {
        return geneSize;
    }

    public void setGeneSize(int geneSize) {
        this.geneSize = geneSize;
    }

    public int getMaxIterNum() {
        return maxIterNum;
    }

    public void setMaxIterNum(int maxIterNum) {
        this.maxIterNum = maxIterNum;
    }

    public double getMutationRate() {
        return mutationRate;
    }

    public void setMutationRate(double mutationRate) {
        this.mutationRate = mutationRate;
    }

    public int getMaxMutationNum() {
        return maxMutationNum;
    }

    public void setMaxMutationNum(int maxMutationNum) {
        this.maxMutationNum = maxMutationNum;
    }

    public int getGeneration() {
        return generation;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    public double getBestScore() {
        return bestScore;
    }

    public void setBestScore(double bestScore) {
        this.bestScore = bestScore;
    }

    public double getWorstScore() {
        return worstScore;
    }

    public void setWorstScore(double worstScore) {
        this.worstScore = worstScore;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }

    public int[] getX() {
        return x;
    }

    public void setX(int[] x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getGeneI() {
        return geneI;
    }

    public void setGeneI(int geneI) {
        this.geneI = geneI;
    }

    public Chromosome getBest_chromosome() {
        return best_chromosome;
    }

    public void setBest_chromosome(Chromosome best_chromosome) {
        this.best_chromosome = best_chromosome;
    }

    public String getFitTarget() {
        return FitTarget;
    }

    public void setFitTarget(String fitTarget) {
        FitTarget = fitTarget;
    }
}
