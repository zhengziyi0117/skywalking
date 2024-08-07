package one.convert;

import java.util.ArrayList;
import java.util.List;

public class FrameTree {

    private String frame;
    private long total;
    private long self;
    private List<FrameTree> children;

    public FrameTree(Frame frame, String[] key2frame) {
        int titleIndex = frame.getTitleIndex();
        this.frame = key2frame[titleIndex];
        this.total = frame.total;
        this.self = frame.self;
    }

    public static FrameTree buildTree(Frame frame, String[] key2frame){
        if (frame == null) return null;

        FrameTree frameTree = new FrameTree(frame, key2frame);
        // has children?
        if (!frame.isEmpty()) {
            frameTree.children = new ArrayList<>(frame.size());
            // build tree
            for (Frame childFrame : frame.values()) {
                FrameTree childFrameTree = buildTree(childFrame, key2frame);
                frameTree.children.add(childFrameTree);
            }
        }
        return frameTree;
    }

    public String getFrame() {
        return frame;
    }

    public long getTotal() {
        return total;
    }

    public long getSelf() {
        return self;
    }

    public List<FrameTree> getChildren() {
        return children;
    }
}
