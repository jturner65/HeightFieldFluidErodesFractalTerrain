function U = femcodeJT()

    % %BUILD K MATRIX AND F VECTOR : 
    % K matrix : Stiffness matrix - defines finite element function across mesh
    % F vector : model vector
    Npts=size(pts,1);                           % number of points
    %ht = ones(size(tri,1),1);                  % 1 ht element per tri
    K=sparse(Npts,Npts);                        % Start stiffness matrix as sparse matrix, since will be mostly 0
    F=zeros(Npts,1);                            % load vector F to hold integrals of phi's times load f(x,y)

    [K,F] = solvePoisFE(pts, tri, ht);

    bndFunc = 0;
    [Kb,Fb] = setDirchBnds(pts, bnd, K, F, bndFunc);
    % % SOLVE POISSON EQ
    U=Kb\Fb;                                    % The FEM approximation is U1 phi1 + ... + UNphiN

    % % PLOT
    % Plot the FEM approximation U(x,y) with values U_1 to U_N at the nodes 
    trisurf(tri,pts(:,1),pts(:,2),0*pts(:,1),U,'edgecolor','k','facecolor','interp');
    %view(2),axis equal,colorbar
    view(2),axis([-1 1 -1 1]),axis equal,colorbar

end
