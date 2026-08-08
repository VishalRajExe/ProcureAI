import json
import re
from pathlib import Path
from graphify.build import build_from_json
from graphify.cluster import cluster, score_all
from graphify.analyze import god_nodes, surprising_connections, suggest_questions
from graphify.report import generate
from graphify.export import to_json

detect_path = Path('graphify-out/.graphify_detect.json')
if not detect_path.exists():
    from graphify.detect import detect
    detect_res = detect(Path('.'))
    detect_path.write_text(json.dumps(detect_res, ensure_ascii=False), encoding='utf-8')
    detect_data = detect_res
else:
    detect_data = json.loads(detect_path.read_text(encoding='utf-8'))

nodes = []
edges = []
seen_nodes = set()

md_files = [Path(p) for p in detect_data.get('files', {}).get('document', []) if p.endswith('.md')]

for file_path in md_files:
    if not file_path.exists():
        continue
    try:
        rel_path = str(file_path.relative_to(Path('.'))).replace('\\', '/')
    except ValueError:
        rel_path = str(file_path).replace('\\', '/')
        
    file_node_id = f"doc:{rel_path}"
    if file_node_id not in seen_nodes:
        nodes.append({
            'id': file_node_id,
            'label': file_path.name,
            'type': 'document',
            'source_file': rel_path
        })
        seen_nodes.add(file_node_id)
    
    try:
        content = file_path.read_text(encoding='utf-8', errors='ignore')
        # Find markdown links [title](path.md)
        links = re.findall(r'\[([^\]]+)\]\(([^)]+)\)', content)
        for text, link in links:
            if link.endswith('.md') and not link.startswith('http'):
                clean_link = link.lstrip('./')
                target_node_id = f"doc:{clean_link}"
                if target_node_id not in seen_nodes:
                    nodes.append({
                        'id': target_node_id,
                        'label': Path(clean_link).name,
                        'type': 'document',
                        'source_file': clean_link
                    })
                    seen_nodes.add(target_node_id)
                edges.append({
                    'source': file_node_id,
                    'target': target_node_id,
                    'relation': 'REFERENCES',
                    'confidence': 1.0,
                    'type': 'EXTRACTED'
                })
    except Exception as e:
        print(f"Error reading {file_path}: {e}")

extraction = {
    'nodes': nodes,
    'edges': edges,
    'input_tokens': 0,
    'output_tokens': 0
}

Path('graphify-out/.graphify_extract.json').write_text(json.dumps(extraction, indent=2, ensure_ascii=False), encoding='utf-8')

G = build_from_json(extraction, root='.', directed=False)
print(f"Building graph with {G.number_of_nodes()} nodes and {G.number_of_edges()} edges")
if G.number_of_nodes() > 0:
    communities = cluster(G)
    cohesion = score_all(G, communities)
    gods = god_nodes(G)
    surprises = surprising_connections(G, communities)
    labels = {cid: f"Community {cid}" for cid in communities}
    questions = suggest_questions(G, communities, labels)
    wrote = to_json(G, communities, 'graphify-out/graph.json')
    report = generate(G, communities, cohesion, labels, gods, surprises, detect_data, {'input': 0, 'output': 0}, '.', suggested_questions=questions)
    Path('graphify-out/GRAPH_REPORT.md').write_text(report, encoding='utf-8')
    print(f"Graphify pipeline finished! Generated graphify-out/graph.json and GRAPH_REPORT.md ({len(communities)} communities)")
