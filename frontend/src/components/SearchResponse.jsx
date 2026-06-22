export default function SearchResponse({ response }) {
  if (!response) return null;

  const isError = response.message === 'Error';

  return (
    <div
      className="search-response"
      style={isError ? {
        background: 'rgba(239, 68, 68, 0.08)',
        borderColor: 'rgba(239, 68, 68, 0.2)',
      } : {}}
      id="search-response"
    >
      <span className="search-response-icon">
        {isError ? '❌' : '✅'}
      </span>
      <span className="search-response-text">
        {isError ? (
          <>Error: {response.error}</>
        ) : (
          <>
            {response.message} — <span className="search-response-query">"{response.query}"</span>
          </>
        )}
      </span>
    </div>
  );
}
