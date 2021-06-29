import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.util.*;

/**
 * This is the main class for the mapping program. It extends the GUI abstract
 * class and implements all the methods necessary, as well as having a main
 * function.
 * 
 * @author tony
 */
public class Mapper extends GUI {
	public static final Color NODE_COLOUR = new Color(77, 113, 255);
	public static final Color SEGMENT_COLOUR = new Color(130, 130, 130);
	public static final Color HIGHLIGHT_COLOUR = new Color(255, 219, 77);

	// these two constants define the size of the node squares at different zoom
	// levels; the equation used is node size = NODE_INTERCEPT + NODE_GRADIENT *
	// log(scale)
	public static final int NODE_INTERCEPT = 1;
	public static final double NODE_GRADIENT = 0.8;

	// defines how much you move per button press, and is dependent on scale.
	public static final double MOVE_AMOUNT = 100;
	// defines how much you zoom in/out per button press, and the maximum and
	// minimum zoom levels.
	public static final double ZOOM_FACTOR = 1.5;
	public static final double MIN_ZOOM = 1, MAX_ZOOM = 200;

	// how far away from a node you can click before it isn't counted.
	public static final double MAX_CLICKED_DISTANCE = 0.15;

	// these two define the 'view' of the program, ie. where you're looking and
	// how zoomed in you are.
	private Location origin;
	private double scale;

	// our data structures.
	private Graph graph;
	private Trie trie;

	// set of articulation points
	private Set<Node> APs = new HashSet<>();

	// segments for highlighting.
	private List<Segment> shortestPath;

	// initialize start node and target node values.
	private Node startNode = null;
	private Node targetNode = null;

	@Override
	protected void redraw(Graphics g) {
		if (graph != null)
			graph.draw(g, getDrawingAreaDimension(), origin, scale);
	}

	@Override
	protected void onClick(MouseEvent e) {
		Location clicked = Location.newFromPoint(e.getPoint(), origin, scale);
		// find the closest node.
		double bestDist = Double.MAX_VALUE;
		Node closest = null;

		for (Node node : graph.nodes.values()) {
			double distance = clicked.distance(node.location);
			if (distance < bestDist) {
				bestDist = distance;
				closest = node;
			}
		}

		// if it's close enough, highlight it and show some information.
		if (clicked.distance(closest.location) < MAX_CLICKED_DISTANCE) {
			getTextOutputArea().append("\n" + closest);

			if (startNode == null) {
				startNode = closest;
				graph.setHighlight(startNode);
				APs.clear();
				getTextOutputArea().setText("Start node :\n" +closest + "\n\nClick your target node");
			} else {
				targetNode = closest;
				graph.setHighlightedTargetNode(startNode, targetNode);
			}
		}
	}

	@Override
	protected void onSearch() {
		if (trie == null)
			return;

		// get the search query and run it through the trie.
		String query = getSearchBox().getText();
		Collection<Road> selected = trie.get(query);

		// figure out if any of our selected roads exactly matches the search
		// query. if so, as per the specification, we should only highlight
		// exact matches. there may be (and are) many exact matches, however, so
		// we have to do this carefully.
		boolean exactMatch = false;
		for (Road road : selected)
			if (road.name.equals(query))
				exactMatch = true;

		// make a set of all the roads that match exactly, and make this our new
		// selected set.
		if (exactMatch) {
			Collection<Road> exactMatches = new HashSet<>();
			for (Road road : selected)
				if (road.name.equals(query))
					exactMatches.add(road);
			selected = exactMatches;
		}

		// set the highlighted roads.
		graph.setHighlight(selected);

		// now build the string for display. we filter out duplicates by putting
		// it through a set first, and then combine it.
		Collection<String> names = new HashSet<>();
		for (Road road : selected)
			names.add(road.name);
		String str = "";
		for (String name : names)
			str += name + "; ";

		if (str.length() != 0)
			str = str.substring(0, str.length() - 2);
		getTextOutputArea().setText(str);
	}

	@Override
	protected void onMove(Move m) {
		if (m == GUI.Move.NORTH) {
			origin = origin.moveBy(0, MOVE_AMOUNT / scale);
		} else if (m == GUI.Move.SOUTH) {
			origin = origin.moveBy(0, -MOVE_AMOUNT / scale);
		} else if (m == GUI.Move.EAST) {
			origin = origin.moveBy(MOVE_AMOUNT / scale, 0);
		} else if (m == GUI.Move.WEST) {
			origin = origin.moveBy(-MOVE_AMOUNT / scale, 0);
		} else if (m == GUI.Move.ZOOM_IN) {
			if (scale < MAX_ZOOM) {
				// yes, this does allow you to go slightly over/under the
				// max/min scale, but it means that we always zoom exactly to
				// the centre.
				scaleOrigin(true);
				scale *= ZOOM_FACTOR;
			}
		} else if (m == GUI.Move.ZOOM_OUT) {
			if (scale > MIN_ZOOM) {
				scaleOrigin(false);
				scale /= ZOOM_FACTOR;
			}
		}
	}

	/**
	 * Is called whenever a mouse wheel is scrolled. and is passed the
	 * MouseWheelEvent object for that scroll. Users can zoom in or zoom
	 * out the map by scrolling mouse wheel.
	 */
	@Override
	protected void onWheel(MouseWheelEvent e) {
		if(e.getWheelRotation() < 0)
			onMove(Move.ZOOM_IN);
		else if(e.getWheelRotation() > 0)
			onMove(Move.ZOOM_OUT);
	}

	/**
	 * It is called when click A* button. Users can find the shortest path
	 * between start point and target point. The shortest path will be
	 * highlighted, the name of road that the shortest path go through
	 * will be print out and the total length of the shortest path.
	 */
	@Override
	protected void onAStar() {
		if (startNode == null || targetNode == null) {
			getTextOutputArea().setText("Needs to specific both nodes.");
		} else {
			redraw();
			findShortestPath(startNode,targetNode);
			graph.setHighlightedSegments(shortestPath);

			String output = "";
			double totalDistance = 0;
			Map<String, Double> roadMap = new HashMap<>();

			for (Segment seg : shortestPath) {

				double segmentLength = seg.length;
				String roadName = seg.road.name;

				if (roadMap.containsKey(roadName)) {
					double distance = roadMap.get(roadName);
					roadMap.put(roadName, segmentLength + distance);
				} else {
					roadMap.put(roadName, segmentLength);
				}
			}
			// print out the route
			for(Map.Entry<String, Double> path : roadMap.entrySet()){
				output += path.getKey() + ": " + Math.round(path.getValue()*1000.0)/1000.0 + "km \n";
				totalDistance += path.getValue();
			}
			output += "\n Total Distance: " + Math.round(totalDistance*1000.0)/1000.0 + "km";
			getTextOutputArea().append("\n\n" + output);
		}
		startNode = null;
		targetNode = null;
	}

	/**
	 * It is called when select the start point and target point,
	 * and then find the shortest path between these two points.
	 * @param startNode
	 * @param targetNode
	 */
	public void findShortestPath(Node startNode, Node targetNode) {
		// A set of nodes that only add visited ones to it.
		Set<Node> visited = new HashSet<>();
		// Initialize the fringe with only start node.
		Fringe initialFringe = new Fringe(startNode, null, 0, startNode.location.distance(targetNode.location));
		PriorityQueue<Fringe> fringes = new PriorityQueue<Fringe>();
		// Add start node into priority queue.
		fringes.offer(initialFringe);

		while (!fringes.isEmpty()) {
			// Pull the node at the head of the priority queue.
			Fringe fringe = fringes.poll();
			Node currentNode = fringe.getCurrentNode(); // node*
			Node prevNode = fringe.getPreviousNode(); // prev*
			double g_value = fringe.getG_Value(); //g*
			double f_value = fringe.getF_Value(); // f*
			if (!visited.contains(currentNode)) {
				visited.add(currentNode);
				// Set currentNode's "prev" node to the previous current node.
				currentNode.previous = prevNode;
				if (currentNode == targetNode) {
					trackBack(fringe);
					break;
				}
				// In each segment of the current node
				for (Segment seg : currentNode.segments) {
					// estimate the one-way roads.
					if (!(seg.road.oneway == 1 && seg.end == currentNode)) {
						Node neigh;
						// for outgoing neighbours
						if (currentNode != seg.start) {
							neigh = seg.start;
						} else {
							// Incoming neighbours
							neigh = seg.end;
						}

						if (!visited.contains(neigh)) {
							// g value of neighbour = g value of previous current node + segment length between current node and previous current node
							double g_neigh = g_value + seg.length;
							// f value of neighbour = g value of neighbour + estimate cost from neigh node to goal node.
							double f_neigh = g_neigh + neigh.location.distance(targetNode.location);
							// Add new element into the fringe.
							Fringe newFringe = new Fringe(neigh, currentNode, g_neigh, f_neigh);
							fringes.offer(newFringe);
						}
					}
				}
			}
		}
	}

	/**
	 * It is called when the A* search find the target node and get the shortest path.
	 * Backtrack from the target node to the start node by using prevNodes, which
	 * achieved by A loop, get the segments between the currentNode and prevNode,
	 * adds them to a list of segments and keep assign the currentNode as prevNode
	 * until the prevNode is null.
	 * @param fringe
	 * @return
	 */
	public List<Segment> trackBack(Fringe fringe) {
		shortestPath = new ArrayList<>();
		Node currentNode = fringe.getCurrentNode();
		Node prevNode = fringe.getPreviousNode();
		while (!(prevNode == null))  {
			//gets the segment between the currentNode and prevNode and adds it to a list of segments.
				shortestPath.add(prevNode.getSegment(currentNode));
				// assign currentNode as prevNode
				Node assign = prevNode;
				prevNode = prevNode.previous;
				currentNode = assign;
		}
		return shortestPath;
	}

	/**
	 * It is called when click A* button. All of the articulation point in the graph
	 * will be found.
	 */
	@Override
	protected void onAPs() {
		// Initialize the articulation points.
		APs.clear();
		for (Node node : graph.nodes.values()) {
			node.depth = Integer.MAX_VALUE;
		}
		Node root = graph.nodes.get(12420);
		root.depth = 0;
		int numSubTrees = 0;

		for (Node neighbour : root.getNeighbours()) {
			if (neighbour.depth == Integer.MAX_VALUE) {
				iterAPs(neighbour, 1, root);
				numSubTrees++;
			}

			if (numSubTrees > 1) {
				APs.add(root);
			}
		}
		redraw();
		graph.setHighlightedAPs(APs);
		getTextOutputArea().setText("There are " + APs.size() + " articulation points in the graph.");
	}

	/**
	 * It is called when finding the articulation point by using iterative method.
	 * @param firstNode
	 * @param depth
	 * @param root
	 */
	public void iterAPs(Node firstNode, int depth, Node root) {
		// Initialise stack as single element
		Stack<IterAPsObject> stack = new Stack<>();
		IterAPsObject first = new IterAPsObject(firstNode, depth, root);
		stack.push(first);
		// repeat until stack is empty
		while (!stack.isEmpty()) {
			// peek the last element of stack
			IterAPsObject current = stack.peek();
			Node currentNode = current.getNode();
			int currentDepth = current.getDepth();
			Node parentNode = current.getParent();

			if (currentNode.depth == Integer.MAX_VALUE) {
				currentNode.depth = currentDepth;
				currentNode.reachBack = currentDepth;
				// set all the neighbours of the currentNode (except its parent firstNode) as children firstNode.
				for (Node neighbour : currentNode.getNeighbours()) {
					if (neighbour != parentNode) {
						currentNode.children.add(neighbour);
					}
				}

			} else if (!currentNode.children.isEmpty()) {
				Node child = currentNode.children.get(0);
				currentNode.children.remove(0);
				// if the child has been visited, then we found the alternative path,
				// and set the reachBack as the minimum of the child's depth or the currentNode's reachBack.
				if (child.depth < Integer.MAX_VALUE) {
					currentNode.reachBack = Math.min(child.depth, currentNode.reachBack);
				} else {
					stack.push(new IterAPsObject(child, currentDepth+ 1, currentNode));
				}
			} else {
				// if the currentNode is not the firstNode, then set the parentNode's reachBack as
				// the minimum of the child's depth or the currentNode's reachBack.
				if (currentNode != firstNode) {
					parentNode.reachBack = Math.min(currentNode.reachBack, parentNode.reachBack);
					if (currentNode.reachBack >= parentNode.depth) {
						APs.add(parentNode);
					}
				}
				stack.remove(current);
			}
		}
	}

	@Override
	protected void onLoad(File nodes, File roads, File segments, File polygons) {
		graph = new Graph(nodes, roads, segments, polygons);
		trie = new Trie(graph.roads.values());
		origin = new Location(-250, 250); // close enough
		scale = 1;
	}

	/**
	 * This method does the nasty logic of making sure we always zoom into/out
	 * of the centre of the screen. It assumes that scale has just been updated
	 * to be either scale * ZOOM_FACTOR (zooming in) or scale / ZOOM_FACTOR
	 * (zooming out). The passed boolean should correspond to this, ie. be true
	 * if the scale was just increased.
	 */
	private void scaleOrigin(boolean zoomIn) {
		Dimension area = getDrawingAreaDimension();
		double zoom = zoomIn ? 1 / ZOOM_FACTOR : ZOOM_FACTOR;

		int dx = (int) ((area.width - (area.width * zoom)) / 2);
		int dy = (int) ((area.height - (area.height * zoom)) / 2);

		origin = Location.newFromPoint(new Point(dx, dy), origin, scale);
	}

	public static void main(String[] args) {
		new Mapper();
	}
}

// code for COMP261 assignments