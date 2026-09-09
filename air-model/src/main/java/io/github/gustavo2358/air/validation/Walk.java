package io.github.gustavo2358.air.validation;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

/** Ordered DFS with one iterator frame per active ancestor, never Java recursion. */
final class Walk {
    private Walk() {}
    interface Visitor<T> {
        boolean enter(T node,long depth);
        default void exit(T node,long depth) {}
    }
    private record Frame<T>(T node,long depth,Iterator<? extends T> children) {}
    static <T> void run(T root,long depth,Function<T,? extends List<? extends T>> children,Visitor<T> visitor) {
        var stack=new ArrayDeque<Frame<T>>();
        if(!visitor.enter(root,depth)) return;
        stack.push(new Frame<>(root,depth,children.apply(root).iterator()));
        while(!stack.isEmpty()) {
            Frame<T> frame=stack.peek();
            if(frame.children().hasNext()) {
                T child=frame.children().next(); long nextDepth=frame.depth()+1;
                if(visitor.enter(child,nextDepth))
                    stack.push(new Frame<>(child,nextDepth,children.apply(child).iterator()));
            } else {
                stack.pop(); visitor.exit(frame.node(),frame.depth());
            }
        }
    }
}
