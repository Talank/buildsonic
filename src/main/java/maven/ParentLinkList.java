package maven;

import java.util.ArrayList;
import java.util.HashSet;

public class ParentLinkList {
    /**
     * 给定POMList,生成若干条父子关系链
     * 给定POM文件，只能由子POM找到父POM，父POM意识不到子POM存在
     * 结点node:存放pom.并且以pom的相对路径作为标识，双指针，分别指向父和子
     * 由Nodes生成若干条父子关系链
     * parentLinkLists：存放父子关系链的表头（父结点）
     */
    private HashSet<Node> parentLinkLists = new HashSet<>();//存放父子链的链表头Node
    private ArrayList<Node> Nodes = new ArrayList<>();//存放所有的Node

    public class Node{
        private POM pom;
        private String name; //用相对路径作为结点标识
        private Node parent;//父指针
        private Node child;//孩子指针

        public Node(POM pom){
            this.pom = pom;
            this.name = pom.getRelativePath();
            this.parent = null;
            this.child = null;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public POM getPom() {
            return pom;
        }

        public void setPom(POM pom) {
            this.pom = pom;
        }

        public Node getParent() {
            return parent;
        }

        public void setParent(Node parent) {
            this.parent = parent;
        }

        public Node getChild() {
            return child;
        }

        public void setChild(Node child) {
            this.child = child;
        }

    }

    public HashSet<Node> getParentLinkLists(ArrayList<POM> pomList){
        initNodes(pomList);
        linkNode();
        setParentLinkLists();
        return parentLinkLists;
    }

    public void initNodes(ArrayList<POM> pomList){
        for (POM pom:pomList) {
            Node node = new Node(pom);
            Nodes.add(node);
        }
    }

    public void linkNode(){
        for (Node node:Nodes) {
            POM pom = node.getPom();
            if (pom.hasParent()){
                //用相对路径来标识
                String fName = pom.getParent().getRelativePath();
                for (Node fnode:Nodes) {
                    if(fnode.getName().equals(fName)){
                        node.setParent(fnode);
                        fnode.setChild(node);
                        break;
                    }
                }
            }
        }
    }

    public void setParentLinkLists(){
        for(Node node:Nodes){
            while(node.getParent()!=null){
                node = node.getParent();
            }
            parentLinkLists.add(node);
        }
    }

}
