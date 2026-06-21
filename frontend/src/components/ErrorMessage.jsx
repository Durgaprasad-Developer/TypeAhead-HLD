import React from 'react';

/**
 * Renders a stylized, readable error message when API requests fail.
 *
 * @param {object} props
 * @param {string} props.message
 */
export default function ErrorMessage({ message }) {
  return (
    <div className="flex items-center gap-2 px-4 py-3 text-sm rounded-lg"
         style={{ background: 'rgba(239, 68, 68, 0.08)', border: '1px solid rgba(239, 68, 68, 0.2)', color: 'var(--color-error)' }}>
      <svg className="w-4 h-4 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
      </svg>
      <span>{message}</span>
    </div>
  );
}
