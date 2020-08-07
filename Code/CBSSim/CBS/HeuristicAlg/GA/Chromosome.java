package org.cloudbus.cloudsim.examples.CBS.HeuristicAlg.GA;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 染色体类，染色体表示迁移问题的一个解
 */
public class Chromosome {
    private int[] gene; // 解的结构 云盘仓库映射数组gene[i] = j 表示第i个云盘被放置在第j个仓库
    private double score; // 单个个体的得分，表示一个解的优劣
    public int gene_length = 0;
    public int gene_range = 0;

    /**
     * 染色体的构造函数，随机产生一个解
     * @param cd_size 解的规模这里指云盘数量
     * @param wh_size 解的取值范围，这里指仓库数量
     */
    public Chromosome(int cd_size,int wh_size) {
        this.gene_range = wh_size;
        this.gene_length = cd_size;
        if (cd_size <= 0 || wh_size <=0)
            return;
        gene = new int[cd_size];
        Random r = new Random();
        for (int i = 0; i < cd_size; i++)
            gene[i] = r.nextInt(wh_size);
    }

    /**
     * 染色体的克隆操作，复制一条染色体
     * @param chromosome 被克隆的染色体
     * @return 克隆出的染色体
     */

    public static Chromosome clone(Chromosome chromosome) {
        if (chromosome == null || chromosome.gene == null)
            return null;
        Chromosome copy = new Chromosome(chromosome.gene_length,chromosome.gene_range);
        copy.gene = new int[chromosome.gene.length];
        copy.score = chromosome.score;
        System.arraycopy(chromosome.gene, 0, copy.gene, 0, chromosome.gene.length);
        return copy;
    }

    /**
     * 交叉操作
     * @param p1:交叉对象1
     * @param p2:交叉对象2
     * @return 交叉后的对象
     */
    public static List<Chromosome> genetic(Chromosome p1, Chromosome p2, double crossRate) {
        if (p1 == null ||p2 == null)
            return null;
        if (p1.gene == null || p2.gene == null) {
            return null;
        }
        if (p1.gene.length != p2.gene.length) {
            return null;
        }

        Chromosome c1 = clone(p1);
        Chromosome c2 = clone(p2);
        if (Math.random() < crossRate) {
            int size = c1.gene.length; // 染色体长度
            int roundA = (int)(Math.random() * size );
            int roundB = (int)(Math.random() * size);
            // min-max 为 染色体上的一段基因
            int max = Math.max(roundA, roundB);
            int min = Math.min(roundA, roundB);

            for (int i = min; i <= max; i++) {  //交换这段基因 得到两个新个体
                int temp = c1.gene[i];
                c1.gene[i] = c2.gene[i];
                c2.gene[i] = temp;
            }
        }
        List<Chromosome> chromosomes = new ArrayList<>();
        chromosomes.add(c1);
        chromosomes.add(c2);
        return chromosomes; //新产生的个体列表
    }

    /**
     * 模拟基因变异
     * @param num 变异的个数
     */

    public void mutation(int num) {
        if (num == 0)
            return;
        int size = gene.length;
        Random r = new Random();
        for (int i = 0; i < num; i++) {
            int at = (int)(Math.random() * size) % size; // 随机一个基因变异
            int tmp = r.nextInt(gene_range);
            gene[at] = tmp;
        }

    }


    public int[] getGene() {
        return gene;
    }

    public void setGene(int[] gene) {
        this.gene = gene;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "Chromosome{" +
                "gene=" + Arrays.toString(gene) +
                ", score=" + score +
                '}';
    }

    public static void main(String[] args) {
        Chromosome chromosome = new Chromosome(3,4);
        chromosome.gene = new int[]{1,2,3,4};
        System.out.println(chromosome.toString());
    }
}
