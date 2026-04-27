import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

interface Props {
  content: string;
  filename: string;
}

function download(content: string, filename: string) {
  const blob = new Blob([content], { type: "text/markdown;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export default function EvaluationView({ content, filename }: Props) {
  return (
    <>
      <div className="result-header">
        <h2>{filename}</h2>
        <button className="download-btn" onClick={() => download(content, filename)}>
          Download .md
        </button>
      </div>
      <div className="markdown">
        <ReactMarkdown remarkPlugins={[remarkGfm]}>{content}</ReactMarkdown>
      </div>
    </>
  );
}
