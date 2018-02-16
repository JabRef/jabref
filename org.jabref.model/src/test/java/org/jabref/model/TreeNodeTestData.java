package org.jabref.model;

public class TreeNodeTestData {
    /**
     * Gets the marked node in the following tree:
     * Root
     * A
     * A (= parent)
     * B (<-- this)
     */
    public static TreeNodeMock getNodeInSimpleTree(TreeNodeMock root) {
        root.addChild(new TreeNodeMock());
        TreeNodeMock parent = new TreeNodeMock();
        root.addChild(parent);
        TreeNodeMock node = new TreeNodeMock();
        parent.addChild(node);
        return node;
    }

    public static TreeNodeMock getNodeInSimpleTree() {
        return getNodeInSimpleTree(new TreeNodeMock());
    }

    /**
     * Gets the marked node in the following tree:
     * Root
     * A
     * A
     * A (= grand parent)
     * B
     * B (= parent)
     * C (<-- this)
     * D (= child)
     * C
     * C
     * C
     * B
     * B
     * A
     */
    public static TreeNodeMock getNodeInComplexTree(TreeNodeMock root) {
        root.addChild(new TreeNodeMock());
        root.addChild(new TreeNodeMock());
        TreeNodeMock grandParent = new TreeNodeMock();
        root.addChild(grandParent);
        root.addChild(new TreeNodeMock());

        grandParent.addChild(new TreeNodeMock());
        TreeNodeMock parent = new TreeNodeMock();
        grandParent.addChild(parent);
        grandParent.addChild(new TreeNodeMock());
        grandParent.addChild(new TreeNodeMock());

        TreeNodeMock node = new TreeNodeMock();
        parent.addChild(node);
        parent.addChild(new TreeNodeMock());
        parent.addChild(new TreeNodeMock());
        parent.addChild(new TreeNodeMock());

        node.addChild(new TreeNodeMock());
        return node;
    }

    public static TreeNodeMock getNodeInComplexTree() {
        return getNodeInComplexTree(new TreeNodeMock());
    }

    /**
     * Gets the marked in the following tree:
     * Root
     * A
     * A
     * A (<- this)
     * A
     */
    public static TreeNodeMock getNodeAsChild(TreeNodeMock root) {
        root.addChild(new TreeNodeMock());
        root.addChild(new TreeNodeMock());
        TreeNodeMock node = new TreeNodeMock();
        root.addChild(node);
        root.addChild(new TreeNodeMock());
        return node;
    }

    /**
     * This is just a dummy class deriving from TreeNode<T> so that we can test the generic class
     */
    public static class TreeNodeMock extends TreeNode<TreeNodeMock> {

        private String name;

        public TreeNodeMock() {
            this("");
        }

        public TreeNodeMock(String name) {
            super(TreeNodeMock.class);
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "TreeNodeMock{" +
                    "name='" + name + '\'' +
                    '}';
        }

        @Override
        public TreeNodeMock copyNode() {
            return new TreeNodeMock(name);
        }
    }
}
