import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * This represents the data structure storing all the roads, nodes, and
 * segments, as well as some information on which nodes and segments should be
 * highlighted.
 * 
 * @author tony
 */
public class Graph {
	// map node IDs to Nodes.
	Map<Integer, Node> nodes = new HashMap<>();
	// map road IDs to Roads.
	Map<Integer, Road> roads;
	// just some collection of Segments.
	Collection<Segment> segments;

	Node highlightedNode;
	Node highlightedTargetNode;
	Collection<Node> highlightedAPs = new ArrayList<>();
	Collection<Road> highlightedRoads = new HashSet<>();
	Collection<Segment> highlightedSegments = new HashSet<>();

	public Graph(File nodes, File roads, File segments, File polygons) {
		this.nodes = Parser.parseNodes(nodes, this);
		this.roads = Parser.parseRoads(roads, this);
		this.segments = Parser.parseSegments(segments, this);
	}

	public void draw(Graphics g, Dimension screen, Location origin, double scale) {
		// a compatibility wart on swing is that it has to give out Graphics
		// objects, but Graphics2D objects are nicer to work with. Luckily
		// they're a subclass, and swing always gives them out anyway, so we can
		// just do this.
		Graphics2D g2 = (Graphics2D) g;

		// draw all the segments.
		g2.setColor(Mapper.SEGMENT_COLOUR);
		for (Segment s : segments)
			s.draw(g2, origin, scale);

		// draw the segments of all highlighted roads.
		g2.setColor(Mapper.HIGHLIGHT_COLOUR);
		g2.setStroke(new BasicStroke(3));
		for (Road road : highlightedRoads) {
			for (Segment seg : road.components) {
				seg.draw(g2, origin, scale);
			}
		}

		// draw the segments of the shortest path.
		if (!highlightedSegments.isEmpty()) {
			g2.setColor(Mapper.HIGHLIGHT_COLOUR);
			g2.setStroke(new BasicStroke(3));
			for (Segment seg : highlightedSegments) {
				seg.draw(g2, origin, scale);
			}
		}

		// draw all the nodes.
		g2.setColor(Mapper.NODE_COLOUR);
		for (Node n : nodes.values())
			n.draw(g2, screen, origin, scale);

		// draw the highlighted node, if it exists.
		if (highlightedNode != null) {
			g2.setColor(Color.RED);
			highlightedNode.draw(g2, screen, origin, scale);
		}

		if (highlightedTargetNode != null) {
			g2.setColor(Color.RED);
			highlightedNode.draw(g2, screen, origin, scale);
			g2.setColor(Color.GREEN);
			highlightedTargetNode.draw(g2, screen, origin, scale);
		}

		for (Node highlightedAP : highlightedAPs) {
			if (highlightedAPs != null) {
				g2.setColor(Mapper.HIGHLIGHT_COLOUR);
				highlightedAP.draw(g2, screen, origin, scale);
			}
		}
	}

	public void setHighlight(Node node) {
		this.highlightedNode = node;
	}

	public void setHighlightedTargetNode(Node startNode, Node targetNode) {
		this.highlightedNode = startNode;
		this.highlightedTargetNode = targetNode;
	}

	public void setHighlightedAPs(Collection<Node> nodes) {
		this.highlightedAPs = nodes;
	}

	public void setHighlight(Collection<Road> roads) {
		this.highlightedRoads = roads;
	}

	public void setHighlightedSegments(Collection<Segment> segments) {
		this.highlightedSegments = segments;
	}
}

// code for COMP261 assignments