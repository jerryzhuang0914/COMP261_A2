import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.util.*;

/**
 * Node represents an intersection in the road graph. It stores its ID and its
 * location, as well as all the segments that it connects to. It knows how to
 * draw itself, and has an informative toString method.
 * 
 * @author tony
 */
public class Node {

	public final int nodeID;
	public final Location location;
	public final Collection<Segment> segments;
	public Node previous;	// node*.prev
	public int reachBack;
	public int depth; // depth*
	public Set<Node> neighbours = new HashSet<>();
	public List<Node> children = new ArrayList<>();

	public Node(int nodeID, double lat, double lon) {
		this.nodeID = nodeID;
		this.location = Location.newFromLatLon(lat, lon);
		this.segments = new HashSet<Segment>();
	}

	public void addSegment(Segment seg) {
		segments.add(seg);
	}

	public void draw(Graphics g, Dimension area, Location origin, double scale) {
		Point p = location.asPoint(origin, scale);

		// for efficiency, don't render nodes that are off-screen.
		if (p.x < 0 || p.x > area.width || p.y < 0 || p.y > area.height)
			return;

		int size = (int) (Mapper.NODE_GRADIENT * Math.log(scale) + Mapper.NODE_INTERCEPT);
		g.fillRect(p.x - size / 2, p.y - size / 2, size, size);
	}

	/**
	 * Return the segment between this node and the other node.
	 * @param other
	 * @return
	 */
	public Segment getSegment(Node other) {
		for (Segment seg : segments) {
			for (Segment otherSeg : other.segments) {
				if (seg.equals(otherSeg)) {
					return seg;
				}
			}
		}
		return null;
	}

	/**
	 * Get the neighbours of current node in its segment.
	 * @return
	 */
	public Set<Node> getNeighbours() {
		for (Segment seg : segments) {
			Node neigh = seg.getNodeNeighbour(this);
			neighbours.add(neigh);
		}
		return neighbours;
	}

	public String toString() {
		Set<String> edges = new HashSet<String>();
		for (Segment s : segments) {
			if (!edges.contains(s.road.name))
				edges.add(s.road.name);
		}

		String str = "ID: " + nodeID + "  loc: " + location + "\nroads: ";
		for (String e : edges) {
			str += e + ", ";
		}
		return str.substring(0, str.length() - 2);
	}
}

// code for COMP261 assignments